package com.example.squishfocus

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.configure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

expect val revenueCatApiKey: String

object RevenueCatManager {
    const val PREMIUM_ENTITLEMENT = "squish_pro"
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
        runCatching {
            Purchases.sharedInstance.awaitCustomerInfo()
                .entitlements
                ?.get(PREMIUM_ENTITLEMENT)
                ?.isActive == true
        }.onSuccess { _isPremium.value = it }
    }
}
