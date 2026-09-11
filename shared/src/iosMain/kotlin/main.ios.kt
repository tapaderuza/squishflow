package com.alvaropassalacqua.squishflow

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSBundle
import kotlin.native.Platform

/** From Info.plist (`RevenueCatApiKey`), so the store key is set in Xcode, not in Kotlin. */
actual val revenueCatApiKey: String
    get() = (NSBundle.mainBundle.objectForInfoDictionaryKey("RevenueCatApiKey") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "test_RrClnxXHuDZzKrnNgwtGSiJgoak"

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
actual val isDebugBuild: Boolean get() = Platform.isDebugBinary

fun MainViewController() = ComposeUIViewController { App() }
