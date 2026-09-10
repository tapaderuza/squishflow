package com.example.squishfocus

import androidx.compose.runtime.Composable

expect object FocusPreferences {
    fun hasCompletedOnboarding(): Boolean
    fun completeOnboarding(selectedPackages: List<String>)
    fun selectedAppCount(): Int
    fun resetOnboarding()
    fun isProtectionEnabled(): Boolean
    fun openProtectionSettings()
}

@Composable
expect fun DistractionPicker(onContinue: (List<String>) -> Unit)