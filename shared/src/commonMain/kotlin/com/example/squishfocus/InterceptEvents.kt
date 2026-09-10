package com.example.squishfocus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object InterceptEvents {
    private val _blockedPackage = MutableStateFlow<String?>(null)
    val blockedPackage = _blockedPackage.asStateFlow()

    fun intercept(packageName: String) { _blockedPackage.value = packageName }
    fun dismiss() { _blockedPackage.value = null }
}

object SelectionEvents {
    private val _requests = MutableStateFlow(0)
    val requests = _requests.asStateFlow()
    fun requestSelection() { _requests.value += 1 }
}

expect object BlockedAppController {
    fun openForOneMinute(packageName: String)
}