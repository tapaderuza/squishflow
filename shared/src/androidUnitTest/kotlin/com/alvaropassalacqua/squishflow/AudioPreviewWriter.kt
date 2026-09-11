package com.alvaropassalacqua.squishflow

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Writes the synthesised gestures to disk so a person can listen to them.
 *
 * A synth can pass every numerical assertion in [SquishSynthTest] and still sound
 * wrong, and nothing in a test suite can tell you that. Running the unit tests
 * drops auditionable WAVs in `shared/build/audio-preview/`, which is the only way
 * to review this code honestly without a device to hand.
 *
 *     ./gradlew :shared:testDebugUnitTest
 *     # then listen to the wav files in shared/build/audio-preview
 */
class AudioPreviewWriter {

    @Test
    fun writeAuditionableWavFiles() {
        val outputDir = File("build/audio-preview").apply { mkdirs() }

        val renders = mapOf(
            "squeeze" to SquishSynth.squeeze(),
            "release" to SquishSynth.release(),
            "squeeze-soft" to SquishSynth.squeeze(intensity = 0.35f),
            // Alternatives, so a person can choose by ear rather than by reading
            // constants: deeper, and bubblier.
            "squeeze-deep" to SquishSynth.render(
                SquishSynth.Voice(
                    durationSeconds = 0.22f,
                    pitchStartHz = 260f, pitchEndHz = 70f, pitchGlide = 0.11f,
                    noiseCutoffScale = 5f, noiseResonance = 0.91f,
                    bursts = floatArrayOf(0f, 0.06f, 0.13f),
                    burstDecay = 28f, toneDecay = 14f, toneMix = 0.62f, noiseMix = 0.55f,
                ), 1f, 1,
            ),
            "squeeze-bubbly" to SquishSynth.render(
                SquishSynth.Voice(
                    durationSeconds = 0.20f,
                    pitchStartHz = 420f, pitchEndHz = 140f, pitchGlide = 0.07f,
                    noiseCutoffScale = 6f, noiseResonance = 0.92f,
                    bursts = floatArrayOf(0f, 0.03f, 0.065f, 0.105f, 0.15f),
                    burstDecay = 44f, toneDecay = 20f, toneMix = 0.45f, noiseMix = 0.72f,
                ), 1f, 3,
            ),
            // A press and its release back to back, as it plays in the app.
            "press-and-release" to (SquishSynth.squeeze() + ShortArray(SquishSynth.SAMPLE_RATE / 5) + SquishSynth.release()),
        )

        renders.forEach { (name, samples) ->
            val file = File(outputDir, "$name.wav")
            file.writeBytes(wav(samples))
            assertTrue(file.length() > 44, "$name.wav has a header and no audio")
        }
    }

    /** 16-bit mono PCM WAV container. */
    private fun wav(samples: ShortArray): ByteArray {
        val dataBytes = samples.size * 2
        val out = java.io.ByteArrayOutputStream(44 + dataBytes)

        fun ascii(text: String) = out.write(text.toByteArray(Charsets.US_ASCII))
        fun int32(v: Int) {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
        }
        fun int16(v: Int) {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
        }

        ascii("RIFF"); int32(36 + dataBytes); ascii("WAVE")
        ascii("fmt "); int32(16); int16(1); int16(1)
        int32(SquishSynth.SAMPLE_RATE)
        int32(SquishSynth.SAMPLE_RATE * 2)
        int16(2); int16(16)
        ascii("data"); int32(dataBytes)
        samples.forEach { int16(it.toInt()) }

        return out.toByteArray()
    }
}
