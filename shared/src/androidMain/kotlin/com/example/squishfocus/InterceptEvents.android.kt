package com.example.squishfocus

import android.content.Intent
import android.os.SystemClock

actual object BlockedAppController {
    actual fun openForOneMinute(packageName: String) {
        val context = FocusPreferences.contextOrNull() ?: return
        context.getSharedPreferences("squish_focus", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("temporary_allowed_package", packageName)
            .putLong("temporary_allowed_until", SystemClock.elapsedRealtime() + 60_000)
            .apply()
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}