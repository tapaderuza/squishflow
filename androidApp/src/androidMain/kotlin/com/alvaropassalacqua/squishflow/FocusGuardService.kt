package com.alvaropassalacqua.squishflow

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class FocusGuardService : AccessibilityService() {
    private var lastInterceptAt = 0L
    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName ||
            packageName == "com.android.systemui" ||
            packageName == "com.android.settings"
        ) return

        val selected = getSharedPreferences("squish_focus", MODE_PRIVATE)
            .getStringSet("selected_apps", emptySet())
            .orEmpty()
        if (packageName !in selected) return

        val preferences = getSharedPreferences("squish_focus", MODE_PRIVATE)
        val allowedPackage = preferences.getString("temporary_allowed_package", null)
        val allowedUntil = preferences.getLong("temporary_allowed_until", 0L)
        if (packageName == allowedPackage && SystemClock.elapsedRealtime() < allowedUntil) return

        val now = SystemClock.elapsedRealtime()
        if (lastPackage == packageName && now - lastInterceptAt < 1_200) return
        lastPackage = packageName
        lastInterceptAt = now

        val launch = packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?: Intent(this, MainActivity::class.java)
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        launch.putExtra("focus_intercepted_package", packageName)
        startActivity(launch)
    }

    override fun onInterrupt() = Unit
}