package com.alvaropassalacqua.squishflow

/**
 * How a squishy is obtained.
 *
 * Every body in the app can be **earned with focus**. Pro buys them now.
 *
 * This is the whole monetisation model, and it is deliberate rather than clever.
 * A focus app that locks its rewards behind a card is working against the thing
 * it claims to want: it profits when you pay, not when you concentrate. Making
 * focused minutes the free currency means the app only gets more rewarding the
 * more it actually works, and the subscription becomes a shortcut for people who
 * would rather not wait — plus the reason there are no ads and no analytics on
 * anyone, paying or not.
 *
 * It also gives a free user something a feature list cannot: a body they can see
 * themselves approaching.
 */
sealed interface MaterialAccess {

    /** Available to everyone from the first launch. */
    data object Free : MaterialAccess

    /** Unlocked by the Pro entitlement. */
    data object Purchased : MaterialAccess

    /** Unlocked by accumulated focus, and kept even if Pro later lapses. */
    data class Earned(val requiredMinutes: Int) : MaterialAccess

    /** Not yet available, with the distance left to close. */
    data class Locked(val requiredMinutes: Int, val focusedMinutes: Int) : MaterialAccess {
        val remainingMinutes: Int get() = (requiredMinutes - focusedMinutes).coerceAtLeast(0)
        val progress: Float
            get() = if (requiredMinutes <= 0) 1f
            else (focusedMinutes.toFloat() / requiredMinutes).coerceIn(0f, 1f)
    }

    val isUsable: Boolean get() = this !is Locked
}

/**
 * Resolve how [this] material stands for a given person.
 *
 * Earning is checked before the entitlement so that a lapsed subscription leaves
 * behind everything the person actually worked for. Taking that away would be
 * punishing them for having focused.
 */
fun SquishyMaterial.accessWith(focusedMinutes: Int, isPremium: Boolean): MaterialAccess = when {
    !isPro -> MaterialAccess.Free
    focusedMinutes >= unlockMinutes -> MaterialAccess.Earned(unlockMinutes)
    isPremium -> MaterialAccess.Purchased
    else -> MaterialAccess.Locked(requiredMinutes = unlockMinutes, focusedMinutes = focusedMinutes)
}

/** The next body a person is working towards, or null once everything is open. */
fun nextMaterialToEarn(focusedMinutes: Int): SquishyMaterial? =
    SquishyMaterial.entries
        .filter { it.isPro && focusedMinutes < it.unlockMinutes }
        .minByOrNull { it.unlockMinutes }

/** Human phrasing for how much focus is left, kept out of the composables. */
fun formatRemaining(minutes: Int): String = when {
    minutes <= 0 -> "ready"
    minutes < 60 -> "$minutes min of focus away"
    minutes % 60 == 0 -> "${minutes / 60} h of focus away"
    else -> "${minutes / 60} h ${minutes % 60} min of focus away"
}

/** Compact form for narrow columns, where the surrounding UI supplies the context. */
fun shortRemaining(minutes: Int): String = when {
    minutes <= 0 -> "Ready"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
