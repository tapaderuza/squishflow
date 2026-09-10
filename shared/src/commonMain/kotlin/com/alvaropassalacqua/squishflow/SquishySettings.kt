package com.alvaropassalacqua.squishflow

/**
 * Small, non-critical preferences that survive a restart.
 *
 * Deliberately separate from [MissionPersistence]: losing which squishy you picked
 * is a papercut, losing a mission in progress is not, and the two should not share
 * a failure mode.
 */
expect object SquishySettings {
    fun loadMaterialKey(): String?
    fun saveMaterialKey(key: String)

    /**
     * Whether the upgrade screen has already introduced itself.
     *
     * This has to outlive the process. Holding it in memory meant the prompt
     * reappeared on every cold start once the session count was high enough,
     * which turns a one-time introduction into nagging.
     */
    fun hasSeenConversionPrompt(): Boolean
    fun markConversionPromptSeen()

    /** Whether the companion has introduced itself. */
    fun hasSeenWelcome(): Boolean
    fun markWelcomeSeen()
}

/** The material to start with, falling back to the free one for a first run. */
fun loadSquishyMaterial(isPremium: Boolean): SquishyMaterial {
    val stored = SquishyMaterial.fromKey(SquishySettings.loadMaterialKey())
    // An expired subscription must not leave a Pro body on screen.
    return if (stored.isPro && !isPremium) SquishyMaterial.free else stored
}
