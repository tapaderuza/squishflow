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

    /**
     * Lifetime focused minutes, the currency that earns squishies.
     *
     * This has to survive reinstall-free process death or the ladder in
     * [MaterialAccess] would silently reset and take earned bodies with it.
     */
    fun focusedMinutes(): Int
    fun addFocusedMinutes(minutes: Int)

    /**
     * Lifetime block counts.
     *
     * The timer's own counters live in memory and reset with the process, which
     * made the journey screen report a lifetime of zero on every cold start.
     */
    fun completedBlocks(): Int
    fun failedBlocks(): Int
    fun recordBlock(completed: Boolean)
}

/** Everything the journey screen reports, read once from storage. */
data class LifetimeStats(
    val focusedMinutes: Int,
    val completedBlocks: Int,
    val failedBlocks: Int,
) {
    val attempts: Int get() = completedBlocks + failedBlocks

    /**
     * Share of started blocks that were finished, or null before the first attempt.
     *
     * Null rather than zero: showing 0% to somebody who has not started yet reads
     * as a judgement they have not earned.
     */
    val consistency: Int? get() = if (attempts == 0) null else completedBlocks * 100 / attempts

    companion object {
        fun load(): LifetimeStats = LifetimeStats(
            focusedMinutes = SquishySettings.focusedMinutes(),
            completedBlocks = SquishySettings.completedBlocks(),
            failedBlocks = SquishySettings.failedBlocks(),
        )
    }
}

/** The material to start with, falling back to the free one for a first run. */
fun loadSquishyMaterial(isPremium: Boolean): SquishyMaterial {
    val stored = SquishyMaterial.fromKey(SquishySettings.loadMaterialKey())
    // An expired subscription must not leave a Pro body on screen.
    return if (stored.isPro && !isPremium) SquishyMaterial.free else stored
}
