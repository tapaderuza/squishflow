package com.example.squishfocus

actual object MissionPersistence {
    private const val KEY = "active_mission_v1"
    private fun prefs() = FocusPreferences.contextOrNull()
        ?.getSharedPreferences("squish_focus", android.content.Context.MODE_PRIVATE)
    actual fun load(): String? = prefs()?.getString(KEY, null)
    actual fun save(value: String) { prefs()?.edit()?.putString(KEY, value)?.apply() }
    actual fun clear() { prefs()?.edit()?.remove(KEY)?.apply() }
}
