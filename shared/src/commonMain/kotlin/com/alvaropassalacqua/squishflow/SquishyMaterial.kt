package com.alvaropassalacqua.squishflow

/**
 * How a squishy is made.
 *
 * These are not skins. Each material retunes the soft-body solver, so a stress
 * ball genuinely resists and snaps back while a water balloon sloshes for a
 * second and a half after you let go. The difference is in the hand, not in a
 * colour swatch, which is the whole reason the physics model exists.
 *
 * Hue is deliberately absent here. Colour carries session state across the entire
 * product — coral is tension, sage is focus, lavender is interruption — so a
 * material that repainted the body would break the one signal the user has
 * learned. Materials differ by *finish* instead: gloss, rim, speckle, translucency.
 */
enum class SquishyMaterial(
    val displayName: String,
    val description: String,
    val isPro: Boolean,
    /**
     * Minutes of accumulated focus that unlock this body for free.
     *
     * The ladder starts short so the mechanic proves itself inside the first day,
     * then lengthens. See [MaterialAccess] for why focus is the free currency.
     */
    val unlockMinutes: Int,
    val tuning: Tuning,
    val finish: Finish,
) {
    JELLY(
        displayName = "Jelly",
        description = "Balanced and translucent.",
        isPro = false,
        unlockMinutes = 0,
        tuning = Tuning(stiffness = 118f, damping = 7.1f, coupling = 46f, maxDisplacement = 0.42f),
        finish = Finish(gloss = 0.30f, rim = 0.10f, speckle = 0.055f, sheen = 0.12f, highlights = 4),
    ),

    STRESS_BALL(
        displayName = "Stress ball",
        description = "Dense foam. Snaps straight back.",
        isPro = true,
        unlockMinutes = 60,
        tuning = Tuning(stiffness = 235f, damping = 13.5f, coupling = 26f, maxDisplacement = 0.26f),
        finish = Finish(gloss = 0.06f, rim = 0.16f, speckle = 0.10f, sheen = 0.04f, highlights = 6),
    ),

    MOCHI(
        displayName = "Mochi",
        description = "Soft, and slow to let a shape go.",
        isPro = true,
        unlockMinutes = 180,
        tuning = Tuning(stiffness = 96f, damping = 11f, coupling = 38f, maxDisplacement = 0.38f),
        finish = Finish(gloss = 0.04f, rim = 0.07f, speckle = 0.14f, sheen = 0.03f, highlights = 5),
    ),

    WATER_BALLOON(
        displayName = "Water balloon",
        description = "Thin skin. Keeps sloshing after you let go.",
        isPro = true,
        unlockMinutes = 420,
        tuning = Tuning(stiffness = 74f, damping = 3.2f, coupling = 92f, maxDisplacement = 0.52f),
        finish = Finish(gloss = 0.46f, rim = 0.22f, speckle = 0.02f, sheen = 0.20f, highlights = 2),
    ),

    BUBBLE(
        displayName = "Bubble",
        description = "Weightless. One touch and it rings.",
        isPro = true,
        unlockMinutes = 900,
        tuning = Tuning(stiffness = 150f, damping = 2.6f, coupling = 110f, maxDisplacement = 0.48f),
        finish = Finish(gloss = 0.55f, rim = 0.30f, speckle = 0.01f, sheen = 0.26f, highlights = 3),
    ),
    ;

    /**
     * Solver constants.
     *
     * [stiffness] is how hard the surface pulls back to round, [damping] how
     * quickly motion bleeds away, [coupling] how far a dent travels to its
     * neighbours, and [maxDisplacement] how far the skin can stretch before the
     * solver clamps it.
     */
    data class Tuning(
        val stiffness: Float,
        val damping: Float,
        val coupling: Float,
        val maxDisplacement: Float,
    )

    /** Surface treatment. Alphas, so every material still reads in both states. */
    data class Finish(
        val gloss: Float,
        val rim: Float,
        val speckle: Float,
        val sheen: Float,
        val highlights: Int,
    )

    companion object {
        val free: SquishyMaterial = JELLY

        /** Stable key for persistence; [name] would break if the enum is reordered. */
        fun fromKey(key: String?): SquishyMaterial =
            entries.firstOrNull { it.name == key } ?: free
    }
}
