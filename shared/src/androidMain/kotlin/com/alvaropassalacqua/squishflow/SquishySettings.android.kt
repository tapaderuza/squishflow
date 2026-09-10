package com.alvaropassalacqua.squishflow

import android.content.Context

actual object SquishySettings {
    private const val FILE = "squish_focus"
    private const val MATERIAL = "squishy_material_v1"
    private const val PROMPT = "conversion_prompt_seen_v1"
    private const val WELCOME = "welcome_seen_v1"

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
}
