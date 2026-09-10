package com.alvaropassalacqua.squishflow

/**
 * The on-device planner.
 *
 * It is a deterministic decomposition, not a model, and the product copy says so.
 * What it has to get right is the thing a generic Pomodoro app gets wrong: turning
 * "finish the client deck and study for two hours" into a short list of blocks a
 * person can look at and believe.
 *
 * Three rules earn most of that:
 *
 * 1. **Never repeat the goal.** The naive version titled every block with the full
 *    sentence, so a plan read as the same row four times and looked broken.
 *    Each task is named once and its later blocks carry only a phase word.
 * 2. **Start smaller than you think.** The opening block is capped, because
 *    starting is the expensive part of focus, not continuing.
 * 3. **Stay short.** A plan longer than six focus blocks is a to-do list, and
 *    people abandon those.
 */
class LocalMissionPlanner : MissionPlanner {

    override suspend fun plan(goal: String, preferredMinutes: Int): FocusMission {
        val cleanGoal = goal.trim().replace(Regex("\\s+"), " ")
        require(cleanGoal.length in MIN_GOAL_LENGTH..MAX_GOAL_LENGTH) { "Describe a concrete goal" }

        val tasks = splitTasks(cleanGoal)
        val blocksPerTask = when (tasks.size) {
            1 -> 3
            2 -> 2
            else -> 1
        }
        val minutes = preferredMinutes.coerceIn(MIN_BLOCK_MINUTES, MAX_BLOCK_MINUTES)

        val focus = tasks.flatMap { task -> blocksFor(task, blocksPerTask, minutes) }
            .take(MAX_FOCUS_BLOCKS)
            .mapIndexed { index, block ->
                // Lower the cost of the first block: a shorter opening is easier to
                // agree to, and the reflection afterwards can lengthen the next one.
                if (index == 0) block.copy(minutes = minOf(block.minutes, OPENING_MINUTES)) else block
            }

        return FocusMission(
            title = tasks.first().shorten(MAX_TITLE_LENGTH),
            originalGoal = cleanGoal,
            blocks = interleaveBreaks(focus),
        )
    }

    /**
     * Split a goal on the connectives people actually use when they list work,
     * in English and Spanish, plus ordinary punctuation.
     */
    private fun splitTasks(goal: String): List<String> = goal
        .split(SEPARATORS)
        .map { it.trim().trim('-', '·').trim() }
        .filter { it.length >= MIN_TASK_LENGTH }
        .take(MAX_TASKS)
        .ifEmpty { listOf(goal) }

    private fun blocksFor(task: String, count: Int, minutes: Int): List<MissionBlock> {
        val label = task.shorten(MAX_TITLE_LENGTH).sentenceCase()
        val phases = PHASES[count] ?: PHASES.getValue(1)
        return phases.map { phase ->
            MissionBlock(
                title = if (phase == null) label else "$label — $phase",
                minutes = minutes,
            )
        }
    }

    /** A break after every second focus block, never trailing the plan. */
    private fun interleaveBreaks(focus: List<MissionBlock>): List<MissionBlock> = buildList {
        focus.forEachIndexed { index, block ->
            add(block)
            val isLast = index == focus.lastIndex
            if (!isLast && (index + 1) % BLOCKS_BETWEEN_BREAKS == 0) {
                add(MissionBlock("Breathe and move", BREAK_MINUTES, BlockKind.BREAK))
            }
        }
    }

    private companion object {
        const val MIN_GOAL_LENGTH = 8
        const val MAX_GOAL_LENGTH = 500
        const val MIN_TASK_LENGTH = 3
        const val MAX_TASKS = 3
        const val MAX_FOCUS_BLOCKS = 6
        const val MAX_TITLE_LENGTH = 32
        const val MIN_BLOCK_MINUTES = 10
        const val MAX_BLOCK_MINUTES = 60
        const val OPENING_MINUTES = 15
        const val BREAK_MINUTES = 10
        const val BLOCKS_BETWEEN_BREAKS = 2

        val SEPARATORS = Regex(
            "\\s+(?:and then|and|then|also|plus|y luego|y|luego|además|y también|también)\\s+|[,;.]+",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Phase words per block count. `null` means the block carries the task name
         * alone, which is what a single-block task should look like.
         */
        val PHASES: Map<Int, List<String?>> = mapOf(
            1 to listOf(null),
            2 to listOf("first pass", "final pass"),
            3 to listOf("first pass", "main pass", "final pass"),
        )
    }
}

/** Trim to [limit] on a word boundary so titles never end mid-word. */
internal fun String.shorten(limit: Int): String {
    if (length <= limit) return this
    val cut = lastIndexOf(' ', limit)
    val end = if (cut > limit / 2) cut else limit
    return take(end).trimEnd(' ', ',', '-', '·')
}

/** Capitalise the first letter without touching acronyms elsewhere in the string. */
internal fun String.sentenceCase(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
