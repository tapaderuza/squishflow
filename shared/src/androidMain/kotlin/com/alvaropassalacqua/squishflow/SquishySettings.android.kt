package com.alvaropassalacqua.squishflow

import android.content.Context

actual object SquishySettings {
    private const val FILE = "squish_focus"
    private const val MATERIAL = "squishy_material_v1"
    private const val PROMPT = "conversion_prompt_seen_v1"
    private const val WELCOME = "welcome_seen_v1"
    private const val FOCUSED = "focused_minutes_v1"
    private const val COMPLETED = "completed_blocks_v1"
    private const val FAILED = "failed_blocks_v1"
    private const val DECLINED = "protection_declined_v1"
    private const val SESSION_DEADLINE = "session_deadline_epoch_v1"
    private const val SESSION_TOTAL = "session_total_seconds_v1"

    private fun prefs() = FocusPreferences.contextOrNull()
        ?.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    actual fun loadMaterialKey(): String? = prefs()?.getString(MATERIAL, null)

    actual fun saveMaterialKey(key: String) {
        prefs()?.edit()?.putString(MATERIAL, key)?.apply()
    }

    actual fun hasSeenConversionPrompt(): Boolean = prefs()?.getBoolean(PROMPT, false) == true

    actual fun markConversionPromptSeen() {
        prefs()?.edit()?.putBoolean(PROMPT, true)?.apply()
    }

    actual fun hasSeenWelcome(): Boolean = prefs()?.getBoolean(WELCOME, false) == true

    actual fun markWelcomeSeen() {
        prefs()?.edit()?.putBoolean(WELCOME, true)?.apply()
    }

    actual fun focusedMinutes(): Int = prefs()?.getInt(FOCUSED, 0) ?: 0

    actual fun addFocusedMinutes(minutes: Int) {
        if (minutes <= 0) return
        val store = prefs() ?: return
        store.edit().putInt(FOCUSED, store.getInt(FOCUSED, 0) + minutes).apply()
    }

    actual fun completedBlocks(): Int = prefs()?.getInt(COMPLETED, 0) ?: 0

    actual fun failedBlocks(): Int = prefs()?.getInt(FAILED, 0) ?: 0

    actual fun recordBlock(completed: Boolean) {
        val store = prefs() ?: return
        val key = if (completed) COMPLETED else FAILED
        store.edit().putInt(key, store.getInt(key, 0) + 1).apply()
    }

    actual fun hasDeclinedProtection(): Boolean = prefs()?.getBoolean(DECLINED, false) == true

    actual fun markProtectionDeclined() {
        prefs()?.edit()?.putBoolean(DECLINED, true)?.apply()
    }

    actual fun loadSession(): Pair<Long, Int>? {
        val store = prefs() ?: return null
        if (!store.contains(SESSION_DEADLINE)) return null
        return store.getLong(SESSION_DEADLINE, 0L) to store.getInt(SESSION_TOTAL, 0)
    }

    actual fun saveSession(deadlineEpochMillis: Long, totalSeconds: Int) {
        // commit() rather than apply(): this is the one write that must land
        // before the process can die, which is the whole reason it exists.
        prefs()?.edit()
            ?.putLong(SESSION_DEADLINE, deadlineEpochMillis)
            ?.putInt(SESSION_TOTAL, totalSeconds)
            ?.commit()
    }

    actual fun clearSession() {
        prefs()?.edit()?.remove(SESSION_DEADLINE)?.remove(SESSION_TOTAL)?.commit()
    }
}
