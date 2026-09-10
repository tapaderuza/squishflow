package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable

/**
 * Whether the person using the app has asked the system for less movement.
 *
 * Squishy is built out of motion, so this is not a setting the app can ignore.
 * When it is on, the body still deforms under a finger — that is the interaction,
 * not decoration — but it stops breathing on its own, stops ringing when the
 * material changes, and stops pulsing at state transitions. Nothing moves that
 * the user did not move.
 */
@Composable
expect fun rememberReducedMotion(): Boolean
