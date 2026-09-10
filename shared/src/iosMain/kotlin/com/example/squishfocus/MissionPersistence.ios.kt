package com.example.squishfocus

import platform.Foundation.NSUserDefaults

actual object MissionPersistence {
    private const val KEY = "squish_active_mission_v1"
    actual fun load(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)
    actual fun save(value: String) { NSUserDefaults.standardUserDefaults.setObject(value, KEY) }
    actual fun clear() { NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY) }
}
