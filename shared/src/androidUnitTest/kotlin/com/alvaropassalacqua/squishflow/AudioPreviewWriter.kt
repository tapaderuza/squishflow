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

        val gap = ShortArray(SquishSynth.SAMPLE_RATE / 5)
        val renders = buildMap {
            put("squeeze", SquishSynth.squeeze())
            put("release", SquishSynth.release())
            put("squeeze-soft", SquishSynth.squeeze(intensity = 0.35f))
            // A press and its release back to back, once per material, as it
            // plays in the app. Jelly is the reference voice.
            SquishyMaterial.entries.forEach { material ->
                val name = material.name.lowercase().replace('_', '-')
                put("press-and-release-$name", SquishSynth.squeeze(material) + gap + SquishSynth.release(material))
            }
        }

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
