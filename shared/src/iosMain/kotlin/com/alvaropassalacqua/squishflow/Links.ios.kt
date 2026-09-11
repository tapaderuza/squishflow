package com.alvaropassalacqua.squishflow

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}

actual val manageSubscriptionUrl: String get() = "https://apps.apple.com/account/subscriptions"
