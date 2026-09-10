package com.alvaropassalacqua.squishflow

import androidx.compose.ui.graphics.Color

/**
 * Squishflow's palette.
 *
 * The names describe each token's *role*, not its brightness: the focus surface is
 * a near-black ground so the body is the only thing emitting light in the room,
 * which is why [Cream] is dark and [Ink] is light. Mission and planning screens
 * invert this to the paper tones and read as the calm half of the app.
 *
 * Hue carries state, deliberately and consistently: [Coral] is tension before a
 * block starts, [Sage] is settled focus, [Lavender] is an interruption. Nothing in
 * the product uses red for failure.
 */
internal val Cream = Color(0xFF0C0E0D)
internal val Ink = Color(0xFFF2F0E9)
internal val Muted = Color(0xFF969991)
internal val Sage = Color(0xFF8DD6AA)
internal val Coral = Color(0xFFFF806C)
internal val Lavender = Color(0xFFC5A8F2)
internal val SoftWhite = Color(0xFF171A18)

/** Body fill for each state, brightest at the centre of the radial gradient. */
internal fun SquishyState.bodyTint(): Color = when (this) {
    SquishyState.TENSE -> Color(0xFFFF806C)
    SquishyState.RELAXING -> Color(0xFF8DD6AA)
    SquishyState.COMPRESSED -> Color(0xFFC5A8F2)
}

/** Shaded edge of the body, used at the far end of the same gradient. */
internal fun SquishyState.bodyShadow(): Color = when (this) {
    SquishyState.TENSE -> Color(0xFFB9473B)
    SquishyState.RELAXING -> Color(0xFF3E8960)
    SquishyState.COMPRESSED -> Color(0xFF77569F)
}
