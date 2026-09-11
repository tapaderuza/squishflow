package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable

expect object FocusPreferences {
    fun hasCompletedOnboarding(): Boolean
    fun completeOnboarding(selectedPackages: List<String>)
    fun selectedAppCount(): Int
    fun resetOnboarding()
    fun isProtectionEnabled(): Boolean
    fun openProtectionSettings()
}

/**
 * Pick the apps to protect.
 *
 * [allowance] is the most that may be selected, or null for no limit (see
 * [protectedAppAllowance]); reaching it offers Pro through [onUpgrade] rather
 * than silently refusing the tap. The upgrade screens replace this one, so the
 * selection in progress is held by the caller as [draft] and reported through
 * [onDraftChanged]; otherwise coming back from the pitch lost every tick.
 */
@Composable
expect fun DistractionPicker(
    allowance: Int?,
    draft: List<String>?,
    onDraftChanged: (List<String>) -> Unit,
    onUpgrade: () -> Unit,
    onContinue: (List<String>) -> Unit,
)