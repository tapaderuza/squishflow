package com.alvaropassalacqua.squishflow

import platform.Foundation.NSUserDefaults

actual object SquishySettings {
    private const val MATERIAL = "squish_material_v1"
    private const val PROMPT = "squish_conversion_prompt_v1"
    private const val WELCOME = "squish_welcome_v1"
    private const val FOCUSED = "squish_focused_minutes_v1"
    private const val COMPLETED = "squish_completed_blocks_v1"
    private const val FAILED = "squish_failed_blocks_v1"

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
}
