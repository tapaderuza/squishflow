package com.alvaropassalacqua.squishflow

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

    /**
     * Whether the person declined the Accessibility setup.
     *
     * Held in memory this reset on every launch, so somebody who chose "timer
     * only" was asked again every single time they opened the app. The header
     * offers protection whenever they want it; the full-screen prompt should ask
     * once.
     */
    fun hasDeclinedProtection(): Boolean
    fun markProtectionDeclined()

    /**
     * The day the app was last opened, as days since the epoch, or null the first time.
     *
     * Kept so a return after a gap can be greeted as a return rather than as
     * another Tuesday. It is the only thing the app knows about absence, and it
     * is deliberately not a streak: nothing counts the gap against anybody.
     */
    fun lastOpenedEpochDay(): Long?
    fun markOpened(epochDay: Long)

    /** The running block, as (deadline epoch millis, total seconds), or null. */
    fun loadSession(): Pair<Long, Int>?
    fun saveSession(deadlineEpochMillis: Long, totalSeconds: Int)
    fun clearSession()
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

/**
 * Whole days since the app was last opened, or null when it never was.
 *
 * Calendar days rather than 24-hour periods, so opening it late one night and
 * early the next morning counts as consecutive days, which is how people count.
 */
fun daysAway(lastOpenedEpochDay: Long?, todayEpochDay: Long): Int? =
    lastOpenedEpochDay?.let { (todayEpochDay - it).coerceAtLeast(0L).toInt() }

/** Record this launch and report how long it has been since the previous one. */
@OptIn(ExperimentalTime::class)
fun noteLaunchAndDaysAway(): Int? {
    val today = Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY
    val away = daysAway(SquishySettings.lastOpenedEpochDay(), today)
    SquishySettings.markOpened(today)
    return away
}

private const val MILLIS_PER_DAY = 86_400_000L

/** The material to start with, falling back to the free one for a first run. */
fun loadSquishyMaterial(isPremium: Boolean): SquishyMaterial =
    restoreMaterial(SquishySettings.loadMaterialKey(), SquishySettings.focusedMinutes(), isPremium)

/**
 * The stored body, if the person still has the right to it.
 *
 * Checked through [accessWith] rather than the subscription alone: an expired
 * subscription must not leave a Pro body on screen, but a body *earned* with
 * focus is theirs regardless, and an earlier version of this reset it to Jelly
 * on every cold start, which is the opposite of what earning is for.
 */
internal fun restoreMaterial(storedKey: String?, focusedMinutes: Int, isPremium: Boolean): SquishyMaterial {
    val stored = SquishyMaterial.fromKey(storedKey)
    return if (stored.accessWith(focusedMinutes, isPremium).isUsable) stored else SquishyMaterial.free
}
