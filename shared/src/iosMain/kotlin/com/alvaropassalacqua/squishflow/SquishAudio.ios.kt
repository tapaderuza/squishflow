package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

@Composable
actual fun rememberSquishAudio(): SquishAudio = remember { IosSquishAudio() }

/**
 * Wraps the synthesised PCM in a WAV container and hands it to AVAudioPlayer.
 *
 * The players are built once and kept primed, because the first play of a cold
 * player arrives late enough to break the link between the finger and the sound.
 *
 * The session category is Ambient on purpose: it is silenced by the hardware ring
 * switch and never interrupts whatever the person is already listening to. A
 * focus companion that stops somebody's music to make a squelch has misunderstood
 * its job.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosSquishAudio : SquishAudio {

    private val players: Map<SquishyMaterial, Map<SquishGesture, List<AVAudioPlayer>>> = runCatching {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, null)
        AVAudioSession.sharedInstance().setActive(true, null)

        SquishyMaterial.entries.associateWith { material ->
            mapOf(
                SquishGesture.SQUEEZE to buildPlayers(SquishSynth.squeeze(material)),
                SquishGesture.RELEASE to buildPlayers(SquishSynth.release(material)),
            )
        }
    }.getOrDefault(emptyMap())

    private var next = 0

    private fun buildPlayers(samples: ShortArray): List<AVAudioPlayer> {
        val data = wavData(samples) ?: return emptyList()
        return List(POOL_SIZE) {
            AVAudioPlayer(data = data, error = null).apply { prepareToPlay() }
        }
    }

    override fun play(gesture: SquishGesture, intensity: Float, material: SquishyMaterial) {
        val pool = players[material]?.get(gesture)?.takeIf { it.isNotEmpty() } ?: return
        val player = pool[next % pool.size]
        next++
        runCatching {
            player.stop()
            player.currentTime = 0.0
            player.volume = intensity.coerceIn(0f, 1f)
            player.play()
        }
    }

    /** Minimal 16-bit mono PCM WAV container around [samples]. */
    private fun wavData(samples: ShortArray): NSData? {
        if (samples.isEmpty()) return null
        val dataBytes = samples.size * 2
        val bytes = ByteArray(WAV_HEADER_BYTES + dataBytes)
        var at = 0

        fun ascii(text: String) {
            text.forEach { bytes[at++] = it.code.toByte() }
        }

        fun int32(value: Int) {
            bytes[at++] = (value and 0xFF).toByte()
            bytes[at++] = ((value shr 8) and 0xFF).toByte()
            bytes[at++] = ((value shr 16) and 0xFF).toByte()
            bytes[at++] = ((value shr 24) and 0xFF).toByte()
        }

        fun int16(value: Int) {
            bytes[at++] = (value and 0xFF).toByte()
            bytes[at++] = ((value shr 8) and 0xFF).toByte()
        }

        ascii("RIFF"); int32(36 + dataBytes); ascii("WAVE")
        ascii("fmt "); int32(16); int16(1); int16(1)
        int32(SquishSynth.SAMPLE_RATE)
        int32(SquishSynth.SAMPLE_RATE * 2)   // byte rate: mono, 2 bytes per sample
        int16(2); int16(16)
        ascii("data"); int32(dataBytes)

        samples.forEach { int16(it.toInt()) }

        return bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }

    private companion object {
        const val POOL_SIZE = 3
        const val WAV_HEADER_BYTES = 44
    }
}
