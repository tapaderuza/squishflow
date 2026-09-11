package com.alvaropassalacqua.squishflow

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.configure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

expect val revenueCatApiKey: String

/**
 * True for debug binaries, the only place the Test Store may stand in for a
 * store: the SDK itself detects a non-debuggable build with a test key and
 * closes the app with a "Wrong API Key" dialog.
 */
expect val isDebugBuild: Boolean

object RevenueCatManager {
    /** The entitlement as named in the RevenueCat dashboard. */
    const val PREMIUM_ENTITLEMENT = "Squish Pro"
    private var configured = false
    private val _isConfigured = MutableStateFlow(false)
    val isConfigured = _isConfigured.asStateFlow()
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    fun configure(): Boolean {
        if (configured) return true
        if (!isUsableKey(revenueCatApiKey, isDebugBuild)) return false
        Purchases.logLevel = if (isDebugBuild) LogLevel.INFO else LogLevel.WARN
        Purchases.configure(apiKey = revenueCatApiKey)
        configured = true
        _isConfigured.value = true
        return true
    }

    suspend fun refreshEntitlement() {
        if (!configured) return
        runCatching { Purchases.sharedInstance.awaitCustomerInfo() }
            .onSuccess(::apply)
    }

    /**
     * Whether [key] may configure the SDK in this build.
     *
     * The Test Store key simulates purchases and, per RevenueCat, crashes the SDK
     * in production. A release build with it left in gets no store at all, and
     * the app degrades to "everything is earnable" rather than to a crash on
     * the plans screen.
     */
    internal fun isUsableKey(key: String, debug: Boolean): Boolean = when {
        key.isBlank() || key.contains("TU_API_KEY") -> false
        key.startsWith("test_") -> debug
        else -> true
    }

    /**
     * Take the entitlement from a [CustomerInfo] the paywall just handed back.
     *
     * A purchase or a restore returns the fresh customer info with it, so the
     * body unlocks the instant the sheet closes instead of on the next launch.
     */
    fun apply(info: CustomerInfo) {
        _isPremium.value = isPro(info.entitlements.active.keys)
    }

    /**
     * The app sells exactly one thing, so any active entitlement is Pro.
     *
     * Matching only the exact identifier once cost a real purchase: the
     * dashboard named the entitlement with a space and a capital, the code
     * without, and a completed transaction unlocked nothing.
     */
    internal fun isPro(activeEntitlements: Collection<String>): Boolean =
        PREMIUM_ENTITLEMENT in activeEntitlements || activeEntitlements.isNotEmpty()
}
