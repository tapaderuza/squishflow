package com.alvaropassalacqua.squishflow

/**
 * Why the upgrade screen opened, so it can open with the right sentence.
 *
 * The same screen serves four moments: a trial body has just left the hand, the
 * free protection limit was hit, the shelf's "have them all now" was tapped,
 * and the quiet introduction after a few finished blocks. Leading every one
 * of them with the same headline wastes the one thing the screen knows.
 */
sealed interface PitchReason {
    /** A locked body was in the hand for six seconds and has just been returned. */
    data class Trial(val material: SquishyMaterial, val minutesToEarn: Int) : PitchReason
    /** The fourth app could not be protected on the free plan. */
    data object ProtectionLimit : PitchReason
    /** Opened from the shelf or the header on purpose. */
    data object Shelf : PitchReason
    /** The one-time introduction after a few completed blocks. */
    data object Introduction : PitchReason
}

/** Headline and subtitle for the upgrade screen. */
data class PitchCopy(val title: String, val subtitle: String)

fun pitchCopy(reason: PitchReason): PitchCopy = when (reason) {
    is PitchReason.Trial -> PitchCopy(
        title = "Keep the ${reason.material.displayName.lowercase()}.",
        subtitle = if (reason.minutesToEarn > 0)
            "You can earn it with ${formatMinutes(reason.minutesToEarn)} of focus. Pro puts it on the shelf now."
        else
            "You can earn it with focus. Pro puts it on the shelf now.",
    )
    PitchReason.ProtectionLimit -> PitchCopy(
        title = "Protect every app.",
        subtitle = "Free protects $FREE_PROTECTED_APPS. Pro has no limit, and puts every body on the shelf now.",
    )
    PitchReason.Shelf -> PitchCopy(
        title = "Have them all now.",
        subtitle = "Every squishy unlocks with focused minutes. Pro is for people who would rather not wait.",
    )
    PitchReason.Introduction -> PitchCopy(
        title = "You can earn all of this.",
        subtitle = "Every squishy unlocks with focused minutes. Pro is for people who would rather not wait.",
    )
}

/** "45 min", "3 h", "3 h 29 min". */
internal fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}
