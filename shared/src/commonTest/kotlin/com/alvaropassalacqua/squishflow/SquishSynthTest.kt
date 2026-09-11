package com.alvaropassalacqua.squishflow

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * These cannot tell you whether the sound is *good* — only a pair of ears can do
 * that. What they can do is catch every way a synthesised buffer goes wrong
 * silently: clipping, DC offset, runaway filter resonance, clicks at the edges,
 * and non-determinism across platforms.
 */
class SquishSynthTest {

    private fun ShortArray.peak(): Int = maxOf { abs(it.toInt()) }

    @Test
    fun aSqueezeProducesAudibleAudio() {
        val buffer = SquishSynth.squeeze()

        assertTrue(buffer.isNotEmpty(), "No samples rendered")
        assertTrue(buffer.peak() > Short.MAX_VALUE / 4, "Rendered too quietly to hear: ${buffer.peak()}")
    }

    @Test
    fun nothingClips() {
        // A filter blowing up is inaudible in code review and unmistakable on a
        // speaker, so assert the headroom the normaliser is supposed to leave.
        listOf(SquishSynth.squeeze(), SquishSynth.release()).forEach { buffer ->
            assertTrue(
                buffer.peak() < Short.MAX_VALUE,
                "Buffer reached full scale (${buffer.peak()}) and will distort",
            )
        }
    }

    @Test
    fun bothGesturesLastLongEnoughToRegisterAndShortEnoughToNotLag() {
        listOf(SquishSynth.squeeze(), SquishSynth.release()).forEach { buffer ->
            val ms = buffer.size * 1000 / SquishSynth.SAMPLE_RATE
            assertTrue(ms in 80..250, "A gesture sound of ${ms}ms will read as a glitch or a delay")
        }
    }

    @Test
    fun startsAndEndsNearSilence() {
        // A buffer that begins or ends at amplitude clicks on every playback.
        listOf(SquishSynth.squeeze(), SquishSynth.release()).forEach { buffer ->
            val headroom = Short.MAX_VALUE / 8
            assertTrue(abs(buffer.first().toInt()) < headroom, "Clicks on start: ${buffer.first()}")
            assertTrue(abs(buffer.last().toInt()) < headroom, "Clicks on end: ${buffer.last()}")
        }
    }

    @Test
    fun energyDecaysRatherThanSustaining() {
        val buffer = SquishSynth.squeeze()
        val firstQuarter = buffer.take(buffer.size / 4).maxOf { abs(it.toInt()) }
        val lastQuarter = buffer.takeLast(buffer.size / 4).maxOf { abs(it.toInt()) }

        assertTrue(
            lastQuarter < firstQuarter / 3,
            "A soft body absorbs its own energy: $firstQuarter then $lastQuarter",
        )
    }

    @Test
    fun hasNoDcOffset() {
        // Constant offset wastes headroom and thumps the speaker on each play.
        val buffer = SquishSynth.squeeze()
        val mean = buffer.sumOf { it.toInt() }.toDouble() / buffer.size

        assertTrue(abs(mean) < Short.MAX_VALUE * 0.05, "DC offset of $mean")
    }

    @Test
    fun intensityScalesLoudness() {
        val quiet = SquishSynth.squeeze(intensity = 0.25f)
        val loud = SquishSynth.squeeze(intensity = 1f)

        assertTrue(quiet.peak() < loud.peak(), "Intensity should control level")
        assertEquals(quiet.size, loud.size, "Intensity should not change duration")
    }

    @Test
    fun silenceIsSilence() {
        assertEquals(0, SquishSynth.squeeze(intensity = 0f).size)
    }

    @Test
    fun renderingIsDeterministic() {
        // The same seed has to give the same waveform on every platform, or the
        // app sounds different on Android and iOS for no reason.
        assertTrue(SquishSynth.squeeze().contentEquals(SquishSynth.squeeze()))
        assertTrue(SquishSynth.release().contentEquals(SquishSynth.release()))
    }

    @Test
    fun squeezeAndReleaseAreDifferentSounds() {
        val squeeze = SquishSynth.squeeze()
        val release = SquishSynth.release()

        assertTrue(
            !squeeze.contentEquals(release.copyOf(squeeze.size)),
            "Compression and rebound should not sound identical",
        )
    }

    @Test
    fun theFilterStaysStableAcrossItsWholeSweep() {
        val filter = StateVariableFilter()
        val noise = SeededNoise(7)
        var peak = 0f
        // Well past the useful range, including the frequencies where a naive
        // implementation of this topology self-oscillates.
        repeat(4_000) { i ->
            val cutoff = 20f + i.toFloat() / 4_000f * 12_000f
            peak = maxOf(peak, abs(filter.process(noise.next(), cutoff, resonance = 0.9f)))
        }

        assertTrue(peak < 100f, "Filter ran away to $peak")
    }
}


/**
 * A material's voice comes from its physics, so these pin the direction of each
 * mapping rather than a number: if somebody makes the stress ball stiffer, it
 * should get higher, and the test should keep passing.
 */
class MaterialVoiceTest {

    private fun ShortArray.peak(): Int = maxOf { abs(it.toInt()) }

    @Test
    fun jellyIsTheReferenceVoice() {
        assertTrue(SquishSynth.squeeze(SquishyMaterial.JELLY).contentEquals(SquishSynth.squeeze()))
    }

    @Test
    fun aStifferBodyIsHigherAndShorter() {
        val jelly = SquishSynth.voiceFor(SquishyMaterial.JELLY, SquishGesture.SQUEEZE)
        val foam = SquishSynth.voiceFor(SquishyMaterial.STRESS_BALL, SquishGesture.SQUEEZE)

        assertTrue(foam.pitchStartHz > jelly.pitchStartHz, "Dense foam should sound higher")
        assertTrue(foam.durationSeconds < jelly.durationSeconds, "and snap back sooner")
        assertEquals(1, foam.bursts.size, "Foam does not squelch")
    }

    @Test
    fun aBubbleRingsLongerThanAnything() {
        val bubble = SquishSynth.squeeze(SquishyMaterial.BUBBLE).size
        SquishyMaterial.entries.filter { it != SquishyMaterial.BUBBLE }.forEach { other ->
            assertTrue(bubble > SquishSynth.squeeze(other).size, "Bubble should outlast ${other.displayName}")
        }
    }

    @Test
    fun aWaterBalloonIsWetterThanJelly() {
        val jelly = SquishSynth.voiceFor(SquishyMaterial.JELLY, SquishGesture.SQUEEZE)
        val balloon = SquishSynth.voiceFor(SquishyMaterial.WATER_BALLOON, SquishGesture.SQUEEZE)

        assertTrue(balloon.noiseResonance > jelly.noiseResonance, "Liquid rings wetter")
        assertTrue(balloon.bursts.size > jelly.bursts.size, "and squelches in more stages")
    }

    @Test
    fun everyMaterialRendersCleanlyInBothGestures() {
        SquishyMaterial.entries.forEach { material ->
            listOf(SquishSynth.squeeze(material), SquishSynth.release(material)).forEach { buffer ->
                val ms = buffer.size * 1000 / SquishSynth.SAMPLE_RATE
                assertTrue(ms in 80..480, "${material.displayName}: ${ms}ms")
                assertTrue(buffer.peak() in (Short.MAX_VALUE / 4) until Short.MAX_VALUE.toInt(), "${material.displayName}: peak ${buffer.peak()}")
                assertTrue(abs(buffer.last().toInt()) < Short.MAX_VALUE / 8, "${material.displayName} clicks on end")
            }
        }
    }
}
