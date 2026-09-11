package com.alvaropassalacqua.squishflow

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.configure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

expect val revenueCatApiKey: String

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
        if (revenueCatApiKey.contains("TU_API_KEY") || revenueCatApiKey.isBlank()) return false
        Purchases.logLevel = LogLevel.INFO
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
