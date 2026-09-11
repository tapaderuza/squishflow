package com.alvaropassalacqua.squishflow

import platform.Foundation.NSUserDefaults

actual object SquishySettings {
    private const val MATERIAL = "squish_material_v1"
    private const val PROMPT = "squish_conversion_prompt_v1"
    private const val WELCOME = "squish_welcome_v1"
    private const val FOCUSED = "squish_focused_minutes_v1"
    private const val COMPLETED = "squish_completed_blocks_v1"
    private const val FAILED = "squish_failed_blocks_v1"
    private const val DECLINED = "squish_protection_declined_v1"
    private const val LAST_OPENED = "squish_last_opened_epoch_day_v1"
    private const val FOCUS_DAYS = "squish_focus_days_v1"
    private const val SESSION_DEADLINE = "squish_session_deadline_v1"
    private const val SESSION_TOTAL = "squish_session_total_v1"

    actual fun loadMaterialKey(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(MATERIAL)

    actual fun saveMaterialKey(key: String) {
        NSUserDefaults.standardUserDefaults.setObject(key, MATERIAL)
    }

    actual fun hasSeenConversionPrompt(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(PROMPT)

    actual fun markConversionPromptSeen() {
        NSUserDefaults.standardUserDefaults.setBool(true, PROMPT)
    }

    actual fun hasSeenWelcome(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(WELCOME)

    actual fun markWelcomeSeen() {
        NSUserDefaults.standardUserDefaults.setBool(true, WELCOME)
    }

    actual fun focusedMinutes(): Int =
        NSUserDefaults.standardUserDefaults.integerForKey(FOCUSED).toInt()

    actual fun addFocusedMinutes(minutes: Int) {
        if (minutes <= 0) return
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setInteger((defaults.integerForKey(FOCUSED) + minutes), FOCUSED)
    }

    actual fun completedBlocks(): Int =
        NSUserDefaults.standardUserDefaults.integerForKey(COMPLETED).toInt()

    actual fun failedBlocks(): Int =
        NSUserDefaults.standardUserDefaults.integerForKey(FAILED).toInt()

    actual fun recordBlock(completed: Boolean) {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = if (completed) COMPLETED else FAILED
        defaults.setInteger(defaults.integerForKey(key) + 1, key)
    }

    actual fun hasDeclinedProtection(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(DECLINED)

    actual fun markProtectionDeclined() {
        NSUserDefaults.standardUserDefaults.setBool(true, DECLINED)
    }

    actual fun focusDays(): String? = NSUserDefaults.standardUserDefaults.stringForKey(FOCUS_DAYS)

    actual fun saveFocusDays(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, FOCUS_DAYS)
    }

    actual fun lastOpenedEpochDay(): Long? {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(LAST_OPENED) == null) return null
        return defaults.integerForKey(LAST_OPENED)
    }

    actual fun markOpened(epochDay: Long) {
        NSUserDefaults.standardUserDefaults.setInteger(epochDay, LAST_OPENED)
    }

    actual fun loadSession(): Pair<Long, Int>? {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(SESSION_DEADLINE) == null) return null
        return defaults.integerForKey(SESSION_DEADLINE) to defaults.integerForKey(SESSION_TOTAL).toInt()
    }

    actual fun saveSession(deadlineEpochMillis: Long, totalSeconds: Int) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setInteger(deadlineEpochMillis, SESSION_DEADLINE)
        defaults.setInteger(totalSeconds.toLong(), SESSION_TOTAL)
        defaults.synchronize()
    }

    actual fun clearSession() {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.removeObjectForKey(SESSION_DEADLINE)
        defaults.removeObjectForKey(SESSION_TOTAL)
    }
}
