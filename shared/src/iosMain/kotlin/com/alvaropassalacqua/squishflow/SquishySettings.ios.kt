package com.alvaropassalacqua.squishflow

import platform.Foundation.NSUserDefaults

actual object SquishySettings {
    private const val MATERIAL = "squish_material_v1"
    private const val PROMPT = "squish_conversion_prompt_v1"

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
}
