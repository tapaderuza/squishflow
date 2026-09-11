package com.alvaropassalacqua.squishflow

import android.content.Intent
import android.net.Uri

actual fun openUrl(url: String) {
    val context = FocusPreferences.contextOrNull() ?: return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

actual val manageSubscriptionUrl: String
    get() = "https://play.google.com/store/account/subscriptions?package=com.alvaropassalacqua.squishflow"
