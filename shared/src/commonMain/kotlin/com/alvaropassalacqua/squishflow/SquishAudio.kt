package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** The two sounds the body can make. See [SquishSynth] for how they are built. */
enum class SquishGesture { SQUEEZE, RELEASE }

@Stable
interface SquishAudio {
    /** Play [gesture] in the voice of [material]; see [SquishSynth.voiceFor]. */
    fun play(gesture: SquishGesture, intensity: Float = 1f, material: SquishyMaterial = SquishyMaterial.free)
}

/** Audio that does nothing, for previews and tests. */
object SilentAudio : SquishAudio {
    override fun play(gesture: SquishGesture, intensity: Float, material: SquishyMaterial) = Unit
}

/**
 * A voice for the companion.
 *
 * Deliberately tied to the device's ringer mode rather than to a setting buried
 * in the app: somebody who silenced their phone to concentrate has already told
 * us what they want, and asking again in our own settings screen would be
 * ignoring the answer.
 */
@Composable
expect fun rememberSquishAudio(): SquishAudio
