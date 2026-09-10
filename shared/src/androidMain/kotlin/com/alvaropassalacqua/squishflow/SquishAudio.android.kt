package com.alvaropassalacqua.squishflow

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSquishAudio(): SquishAudio {
    val context = LocalContext.current
    val audio = remember(context) { AndroidSquishAudio(context) }
    DisposableEffect(audio) { onDispose { audio.release() } }
    return audio
}

/**
 * Plays synthesised buffers through a small pool of static [AudioTrack]s.
 *
 * The waveforms are rendered once on construction rather than per tap: a squish
 * has to arrive with the finger, and synthesising 3,000 samples inside a gesture
 * callback would put that work on the frame it needs to be inaudible on.
 *
 * The pool exists because a static track cannot restart instantly while it is
 * still playing; rotating across a few lets a quick double-squish overlap the way
 * two real presses would.
 */
private class AndroidSquishAudio(context: Context) : SquishAudio {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val waveforms: Map<SquishGesture, ShortArray> = mapOf(
        SquishGesture.SQUEEZE to SquishSynth.squeeze(),
        SquishGesture.RELEASE to SquishSynth.release(),
    )

    private val pool: List<AudioTrack> = buildList {
        val longest = waveforms.values.maxOf { it.size }
        repeat(POOL_SIZE) {
            runCatching { newTrack(longest) }.getOrNull()?.let(::add)
        }
    }

    private var next = 0

    private fun newTrack(sampleCount: Int): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                // Sonification rather than media: this is interface feedback, and
                // it should duck and route like a system sound, not like music.
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SquishSynth.SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setBufferSizeInBytes(sampleCount * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    override fun play(gesture: SquishGesture, intensity: Float) {
        if (pool.isEmpty()) return
        // A silenced phone is an answer already given.
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        val samples = waveforms[gesture] ?: return
        val track = pool[next]
        next = (next + 1) % pool.size

        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.write(samples, 0, samples.size)
            track.setVolume(intensity.coerceIn(0f, 1f))
            track.play()
        }
    }

    fun release() {
        pool.forEach { track -> runCatching { track.stop(); track.release() } }
    }

    private companion object {
        const val POOL_SIZE = 3
    }
}
