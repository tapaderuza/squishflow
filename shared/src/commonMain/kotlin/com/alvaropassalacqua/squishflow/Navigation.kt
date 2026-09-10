package com.alvaropassalacqua.squishflow

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Where the app currently is.
 *
 * Routing used to be a `when` that swapped composables instantly, so every move
 * through the app was a hard cut. Naming the destinations lets the transition
 * between any two be chosen from what the move *means* rather than applying one
 * generic fade everywhere.
 *
 * [depth] is how far into the product a destination sits. Comparing the depth of
 * where you were against where you are going is what decides whether the screen
 * rises or falls, which is the whole grammar: forward is up, back is down.
 */
internal sealed interface Destination {
    val depth: Int

    /** Overlays sit above the flow and return you to exactly where you were. */
    val isOverlay: Boolean get() = depth >= OVERLAY_DEPTH

    data object Welcome : Destination { override val depth = 0 }
    data object ChooseDistractions : Destination { override val depth = 1 }
    data object ProtectionSetup : Destination { override val depth = 2 }
    data object Planner : Destination { override val depth = 3 }
    data object MissionReview : Destination { override val depth = 4 }
    data object Focus : Destination { override val depth = 5 }
    data object Break : Destination { override val depth = 5 }

    data object Journey : Destination { override val depth = OVERLAY_DEPTH }
    data object Rescue : Destination { override val depth = OVERLAY_DEPTH }
    data object Reflection : Destination { override val depth = OVERLAY_DEPTH }
    data object PremiumIntro : Destination { override val depth = OVERLAY_DEPTH }
    data object Paywall : Destination { override val depth = OVERLAY_DEPTH + 1 }

    /**
     * A protected app was opened. This one interrupts rather than navigates, so
     * it deliberately breaks the grammar and arrives without motion.
     */
    data object Intervention : Destination { override val depth = INTERRUPTION_DEPTH }

    companion object {
        const val OVERLAY_DEPTH = 100
        const val INTERRUPTION_DEPTH = 900
    }
}

/** Soft in, softer out. Nothing in this app should feel like it snapped. */
private val Gentle = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

private const val DURATION_MS = 300
private const val FADE_MS = 220

/**
 * Pick the transition for a move.
 *
 * Three rules, in order of precedence:
 *
 * 1. **An interruption does not animate.** Being pulled out of a distraction is
 *    supposed to feel like a stop, and sliding it in prettily would soften the
 *    one moment the app wants to be abrupt.
 * 2. **Overlays rise and fall.** They sit on top of the flow and give you back
 *    the screen you left, so they move on their own axis.
 * 3. **Everything else follows depth.** Going deeper rises, going back falls.
 *
 * Under [reducedMotion] every rule collapses to a crossfade: the app still marks
 * that something changed, without moving anything across the screen.
 */
internal fun transitionFor(
    from: Destination,
    to: Destination,
    reducedMotion: Boolean,
): ContentTransform {
    if (reducedMotion) {
        return fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
    }

    if (to == Destination.Intervention || from == Destination.Intervention) {
        return fadeIn(tween(120)) togetherWith fadeOut(tween(120))
    }

    val opening = to.isOverlay && !from.isOverlay
    val closing = from.isOverlay && !to.isOverlay

    return when {
        // An overlay travels a longer distance, because it comes from off-screen
        // rather than from the screen underneath it.
        opening -> slideInVertically(tween(DURATION_MS, easing = Gentle)) { it / 5 } +
            fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))

        closing -> fadeIn(tween(FADE_MS)) togetherWith
            slideOutVertically(tween(DURATION_MS, easing = Gentle)) { it / 5 } +
            fadeOut(tween(FADE_MS))

        to.depth > from.depth -> slideInVertically(tween(DURATION_MS, easing = Gentle)) { it / 14 } +
            fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))

        to.depth < from.depth -> slideInVertically(tween(DURATION_MS, easing = Gentle)) { -it / 14 } +
            fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))

        // Same depth, for example a focus block giving way to a break: no
        // direction is implied, so neither screen should claim one.
        else -> fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
    }
}
