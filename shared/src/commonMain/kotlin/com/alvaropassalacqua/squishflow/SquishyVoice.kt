package com.alvaropassalacqua.squishflow

/**
 * Everything the app says on the focus screen.
 *
 * This is one line of text and it is most of the product's personality, so it
 * lives here as a pure function rather than as a `when` buried in a composable:
 * copy that can be unit-tested is copy that can be held to rules.
 *
 * The rules, in the order they matter:
 *
 * 1. **Never imply failure.** An abandoned block is a thing that happened, not a
 *    verdict. Nothing here congratulates, scolds, or uses an exclamation mark.
 * 2. **Plain words.** No "crush it", no "let's go", no streak language. The app
 *    is supposed to lower the cost of starting, and enthusiasm raises it.
 * 3. **Short enough for one line** on a narrow phone, because two lines of
 *    centred body copy fights the companion for attention.
 *
 * Lines rotate by how many blocks somebody has finished, so a returning user is
 * not read the same sentence forever, while the choice stays a pure function of
 * state and therefore testable.
 */
object SquishyVoice {

    /** Longest a line may be before it wraps on a small phone. */
    const val MAX_LENGTH = 44

    private val idle = listOf(
        "Tap. Breathe. Begin.",
        "One block. That is the whole plan.",
        "It gets easier once it has started.",
        "Nothing else has to happen first.",
    )

    private val active = listOf(
        "Just this. Just now.",
        "Nothing else is due.",
        "The block is deciding for you.",
        "Still here.",
    )

    private val returning = listOf(
        "Been a while. Nothing to catch up on.",
        "Back. That was the hard part.",
        "No catching up. Just one block.",
        "The gap does not count. This does.",
    )

    private val closing = listOf(
        "Last minute. Let it finish.",
        "Nearly. Stay with it.",
        "Sixty seconds. Nothing to decide.",
        "Almost banked.",
    )

    private val completed = listOf(
        "That is one.",
        "Banked.",
        "That is how it accumulates.",
        "One more than before.",
    )

    private val interrupted = listOf(
        "No guilt. Come back whenever.",
        "That happens. Start again when you want.",
        "The block ended, not the day.",
        "Put it down. Pick it up later.",
    )

    /**
     * Pick the line for the current moment.
     *
     * [minutesToNextBody] is how much focus stands between the person and their
     * next squishy; when that is within touching distance it outranks the idle
     * rotation, because anticipation is more useful than another invitation.
     *
     * [remainingSeconds] does the same job inside a block: the last minute is
     * where most abandoned blocks are abandoned, and it deserves a different
     * sentence from minute three.
     *
     * [daysAway] is how long since the app was last opened. A return after a gap
     * is the moment a streak app would show a broken chain; this one says the
     * gap does not count, because that is the product's whole position.
     */
    fun line(
        state: SquishyState,
        justCompleted: Boolean,
        completedBlocks: Int,
        minutesToNextBody: Int? = null,
        remainingSeconds: Int? = null,
        daysAway: Int? = null,
    ): String {
        val turn = completedBlocks.coerceAtLeast(0)
        val isClosing = remainingSeconds != null && remainingSeconds in 1..CLOSING_SECONDS

        return when {
            state == SquishyState.COMPRESSED -> interrupted.rotate(turn)
            state == SquishyState.RELAXING && isClosing -> closing.rotate(turn)
            state == SquishyState.RELAXING -> active.rotate(turn)
            justCompleted -> completed.rotate(turn)
            completedBlocks == 0 -> "First block. Start smaller than you think."
            daysAway != null && daysAway >= RETURNING_DAYS -> returning.rotate(turn)
            minutesToNextBody != null && minutesToNextBody in 1..ANTICIPATION_MINUTES ->
                "$minutesToNextBody more minutes to the next squishy."
            else -> idle.rotate(turn)
        }
    }

    private fun List<String>.rotate(turn: Int): String = this[turn % size]

    private const val ANTICIPATION_MINUTES = 20

    /** The stretch of a block that gets its own sentences and a quicker breath. */
    const val CLOSING_SECONDS = 60

    /** Days of absence after which opening the app is greeted as a return. */
    const val RETURNING_DAYS = 3
}
