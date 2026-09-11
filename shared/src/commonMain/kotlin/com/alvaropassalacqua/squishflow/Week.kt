package com.alvaropassalacqua.squishflow

/**
 * The last seven days of focus, as minutes per calendar day.
 *
 * Deliberately not a streak. A streak counts the days you did not miss and
 * breaks; this counts the minutes you did and never breaks. The product's
 * line is "come back each time you drift", and a seven-day strip that shows
 * two full days and five empty ones as *two full days* is that line drawn.
 *
 * Stored as one compact string ("20707:45,20708:90") so it needs no schema;
 * days older than [KEEP_DAYS] are dropped on write.
 */
data class FocusWeek(val minutesByDay: Map<Long, Int>, val today: Long) {

    /** Seven entries, oldest first, ending today. */
    val days: List<Int> get() = (6 downTo 0).map { minutesByDay[today - it] ?: 0 }

    val total: Int get() = days.sum()
    val activeDays: Int get() = days.count { it > 0 }
    val peak: Int get() = days.maxOrNull() ?: 0

    /** "3 h 20 min across 4 days", or null when there is nothing to say yet. */
    val summary: String? get() = when {
        total == 0 -> null
        activeDays == 1 -> "${formatMinutes(total)} this week, on one day"
        else -> "${formatMinutes(total)} this week, across $activeDays days"
    }

    companion object {
        const val KEEP_DAYS = 14L

        fun parse(stored: String?, today: Long): FocusWeek {
            val map = stored.orEmpty()
                .split(',')
                .mapNotNull { entry ->
                    val (day, minutes) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                    val d = day.toLongOrNull() ?: return@mapNotNull null
                    val m = minutes.toIntOrNull() ?: return@mapNotNull null
                    d to m
                }
                .toMap()
            return FocusWeek(map, today)
        }

        /** [stored] plus [minutes] on [day], pruned, back in storage form. */
        fun record(stored: String?, day: Long, minutes: Int): String {
            val map = parse(stored, day).minutesByDay.toMutableMap()
            map[day] = (map[day] ?: 0) + minutes
            // toSortedMap is JVM-only; sort the entries by hand for common code.
            return map.filterKeys { it > day - KEEP_DAYS }
                .entries.sortedBy { it.key }
                .joinToString(",") { "${it.key}:${it.value}" }
        }
    }
}
