package com.alvaropassalacqua.squishflow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The voice of the body, synthesised rather than sampled.
 *
 * No audio files ship with the app. A squish is generated from the same numbers
 * that drive the deformation, so the sound and the shape cannot drift apart, and
 * a new material gets a voice by changing constants rather than by commissioning
 * a recording.
 *
 * The first version was noise through a filter over a sine, and the first person
 * to hear it on a real phone said it needed to be *squishier*. What a wet, soft
 * object actually does, and what this version does:
 *
 * - **A pitch that falls.** The "plop" of compression is a tone gliding down fast.
 *   Release is the same glide up: the surface springing back. Without a pitched
 *   component the sound reads as static, however well the noise is filtered.
 * - **Wet resonance.** The noise runs through a filter with high resonance, so
 *   it rings rather than hisses. The cutoff follows the pitch glide.
 * - **Squelching in stages.** Liquid does not move once; the noise envelope has
 *   two or three humps, so a single press sounds like something giving way.
 * - **An exponential decay**, because soft bodies absorb their own energy fast.
 *
 * Everything is deterministic: the noise comes from a seeded generator, so the
 * output is reproducible, unit-testable, and identical on both platforms.
 */
object SquishSynth {

    const val SAMPLE_RATE = 22_050

    /** Compression: the tone falls, the surface gives in stages. */
    fun squeeze(intensity: Float = 1f, seed: Int = 1): ShortArray = render(
        Voice(
            durationSeconds = 0.19f,
            pitchStartHz = 360f, pitchEndHz = 105f, pitchGlide = 0.09f,
            noiseCutoffScale = 5.5f, noiseResonance = 0.90f,
            bursts = floatArrayOf(0f, 0.045f, 0.10f),
            burstDecay = 34f, toneDecay = 17f, toneMix = 0.55f, noiseMix = 0.62f,
        ),
        intensity, seed,
    )

    /** Expansion: the tone rises as the surface snaps back out. */
    fun release(intensity: Float = 1f, seed: Int = 2): ShortArray = render(
        Voice(
            durationSeconds = 0.21f,
            pitchStartHz = 130f, pitchEndHz = 410f, pitchGlide = 0.11f,
            noiseCutoffScale = 6.5f, noiseResonance = 0.86f,
            bursts = floatArrayOf(0f, 0.07f),
            burstDecay = 26f, toneDecay = 13f, toneMix = 0.50f, noiseMix = 0.55f,
        ),
        intensity, seed,
    )

    /**
     * One gesture's worth of constants.
     *
     * @property pitchGlide seconds the tone takes to travel from start to end.
     * @property noiseCutoffScale filter cutoff as a multiple of the current pitch.
     * @property bursts onsets, in seconds, of each squelch hump.
     */
    internal data class Voice(
        val durationSeconds: Float,
        val pitchStartHz: Float,
        val pitchEndHz: Float,
        val pitchGlide: Float,
        val noiseCutoffScale: Float,
        val noiseResonance: Float,
        val bursts: FloatArray,
        val burstDecay: Float,
        val toneDecay: Float,
        val toneMix: Float,
        val noiseMix: Float,
    )

    internal fun render(voice: Voice, intensity: Float, seed: Int): ShortArray {
        val level = intensity.coerceIn(0f, 1f)
        val count = (SAMPLE_RATE * voice.durationSeconds).toInt()
        if (count <= 0 || level == 0f) return ShortArray(0)

        val noise = SeededNoise(seed)
        val filter = StateVariableFilter()
        val samples = FloatArray(count)
        var peak = 0f
        var phase = 0f
        val logRatio = ln(voice.pitchEndHz / voice.pitchStartHz)

        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE

            // The glide is exponential because pitch is heard logarithmically, and
            // it finishes early so the tail sits on the destination note.
            val glide = (t / voice.pitchGlide).coerceIn(0f, 1f)
            val pitch = voice.pitchStartHz * exp(logRatio * glide)

            phase += 2f * PI.toFloat() * pitch / SAMPLE_RATE
            // A slightly rounded sine reads as a soft body rather than a beep.
            val raw = sin(phase)
            val tone = (raw - raw * raw * raw * 0.28f) * exp(-voice.toneDecay * t)

            // Squelch: the sum of a few short humps, each its own decaying burst.
            var envelope = 0f
            for (onset in voice.bursts) {
                val dt = t - onset
                if (dt >= 0f) envelope += attack(dt) * exp(-voice.burstDecay * dt)
            }
            val squelch = filter.process(noise.next(), pitch * voice.noiseCutoffScale, voice.noiseResonance) * envelope

            val value = tone * voice.toneMix + squelch * voice.noiseMix
            samples[i] = value
            peak = max(peak, abs(value))
        }

        // A short fade at the very end, so a buffer cut by the duration cannot click.
        val tail = (SAMPLE_RATE * 0.012f).toInt()
        for (i in max(0, count - tail) until count) {
            samples[i] *= (count - i).toFloat() / tail
        }

        val scale = if (peak > 0f) (level * 0.86f) / peak else 0f
        return ShortArray(count) { i ->
            val v = samples[i] * scale * Short.MAX_VALUE
            min(Short.MAX_VALUE.toFloat(), max(Short.MIN_VALUE.toFloat(), v)).toInt().toShort()
        }
    }

    /** A 3 ms ramp on each burst. Starting at full amplitude would click. */
    private fun attack(dt: Float): Float {
        val ramp = 0.003f
        return if (dt < ramp) dt / ramp else 1f
    }
}

/**
 * Deterministic white noise.
 *
 * A linear congruential generator rather than the platform random: the same seed
 * has to produce the same waveform on Android and iOS, and in tests.
 */
internal class SeededNoise(seed: Int) {
    private var state: Int = if (seed == 0) 1 else seed

    fun next(): Float {
        state = state * 1_664_525 + 1_013_904_223
        return (state shr 8).toFloat() / (1 shl 23).toFloat()
    }
}

/**
 * Chamberlin state-variable filter, band-pass output.
 *
 * Chosen over a one-pole because the resonance is what makes noise sound wet
 * rather than like hiss, and this form lets the cutoff move every sample without
 * recomputing coefficients from scratch.
 */
internal class StateVariableFilter {
    private var low = 0f
    private var band = 0f

    fun process(input: Float, cutoffHz: Float, resonance: Float): Float {
        // Stability limit for this topology is fc < fs/6.
        val safeCutoff = cutoffHz.coerceIn(20f, SquishSynth.SAMPLE_RATE / 6.2f)
        val f = 2f * sin(PI.toFloat() * safeCutoff / SquishSynth.SAMPLE_RATE)
        val q = (1f - resonance.coerceIn(0f, 0.95f)).coerceAtLeast(0.05f)

        low += f * band
        val high = input - low - q * band
        band += f * high
        return band
    }
}
