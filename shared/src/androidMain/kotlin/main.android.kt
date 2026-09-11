package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable

actual val revenueCatApiKey: String get() = com.alvaropassalacqua.squishflow.shared.BuildConfig.REVENUECAT_KEY
actual val isDebugBuild: Boolean get() = com.alvaropassalacqua.squishflow.shared.BuildConfig.DEBUG

@Composable
fun MainView() = App()
