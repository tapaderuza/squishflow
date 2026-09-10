package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * The tactile vocabulary of the app.
 *
 * A squishy that only moves on screen is a picture of a squishy. These accents are
 * what make it feel like an object, so they are treated as a first-class part of
 * the design rather than an afterthought.
 */
enum class HapticAccent {
    /** Finger lands on the body. */
    TOUCH,

    /** Continuous feedback while the body is being deformed. */
    DEFORM,

    /** Finger lifts and the body springs back. */
    RELEASE,

    /** A tension step was released, or a focus block completed. */
    REWARD,

    /** The action was refused, for example starting focus with no mission. */
    REFUSAL,
}

@Stable
interface Haptics {
    fun play(accent: HapticAccent)
}

/** Haptics that do nothing, for previews, tests and reduced-motion users. */
object SilentHaptics : Haptics {
    override fun play(accent: HapticAccent) = Unit
}

@Composable
expect fun rememberHaptics(): Haptics
