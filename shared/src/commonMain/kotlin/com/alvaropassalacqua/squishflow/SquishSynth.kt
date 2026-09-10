package com.alvaropassalacqua.squishflow

import kotlin.math.PI
import kotlin.math.exp
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
 * The recipe is what a wet, soft object actually does:
 *
 * - **Noise through a resonant filter** is the squelch. Sweeping the cutoff
 *   *down* reads as compression, sweeping it *up* reads as the surface springing
 *   back — the same gesture in reverse.
 * - **A low sine** underneath is the mass of the thing, without which a squish
 *   sounds like static.
 * - **An exponential decay** because soft bodies absorb their own energy fast.
 *
 * Everything is deterministic: the noise comes from a seeded generator, so the
 * output is reproducible and unit-testable rather than a different waveform on
 * every call.
 */
object SquishSynth {

    const val SAMPLE_RATE = 22_050

    /** Compression: the cutoff falls, like air being pushed out. */
    fun squeeze(intensity: Float = 1f, seed: Int = 1): ShortArray = render(
        intensity = intensity,
        seed = seed,
        durationSeconds = 0.13f,
        cutoffStart = 2_600f,
        cutoffEnd = 420f,
        bodyHz = 88f,
        decay = 26f,
    )

    /** Expansion: the cutoff rises as the surface snaps back out. */
    fun release(intensity: Float = 1f, seed: Int = 2): ShortArray = render(
        intensity = intensity,
        seed = seed,
        durationSeconds = 0.17f,
        cutoffStart = 520f,
        cutoffEnd = 3_100f,
        bodyHz = 132f,
        decay = 19f,
    )

    /**
     * Render one gesture.
     *
     * [decay] is the exponent of the amplitude envelope: larger values die away
     * faster. [cutoffStart] and [cutoffEnd] bound the filter sweep in hertz.
     */
    internal fun render(
        intensity: Float,
        seed: Int,
        durationSeconds: Float,
        cutoffStart: Float,
        cutoffEnd: Float,
        bodyHz: Float,
        decay: Float,
    ): ShortArray {
        val level = intensity.coerceIn(0f, 1f)
        val count = (SAMPLE_RATE * durationSeconds).toInt()
        if (count <= 0 || level == 0f) return ShortArray(0)

        val noise = SeededNoise(seed)
        val filter = StateVariableFilter()
        val samples = FloatArray(count)
        var peak = 0f

        for (i in 0 until count) {
            val t = i.toFloat() / count

            // Sweep the resonance across the burst. Exponential rather than linear
            // because pitch is heard logarithmically.
            val cutoff = cutoffStart * exp(t * kotlin.math.ln(cutoffEnd / cutoffStart))
            val squelch = filter.process(noise.next(), cutoff, resonance = 0.72f)

            // The body of the object, fading faster than the squelch above it.
            val body = sin(2f * PI.toFloat() * bodyHz * i / SAMPLE_RATE) * exp(-decay * 1.6f * t)

            val envelope = attack(t) * exp(-decay * t)
            val value = (squelch * 0.78f + body * 0.42f) * envelope
            samples[i] = value
            peak = max(peak, kotlin.math.abs(value))
        }

        // Normalise so intensity is the only thing controlling loudness, and a
        // quiet render never arrives as silence.
        val scale = if (peak > 0f) (level * 0.86f) / peak else 0f
        return ShortArray(count) { i ->
            val v = (samples[i] * scale * Short.MAX_VALUE)
            min(Short.MAX_VALUE.toFloat(), max(Short.MIN_VALUE.toFloat(), v)).toInt().toShort()
        }
    }

    /** A 4 ms ramp. Starting at full amplitude would click. */
    private fun attack(t: Float): Float {
        val attackFraction = 0.03f
        return if (t < attackFraction) t / attackFraction else 1f
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
