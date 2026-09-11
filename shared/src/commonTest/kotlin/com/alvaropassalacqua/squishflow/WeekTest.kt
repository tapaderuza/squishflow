package com.alvaropassalacqua.squishflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeekTest {

    @Test
    fun sevenDaysEndingTodayOldestFirst() {
        val week = FocusWeek.parse("100:15,105:45,106:30", today = 106)
        assertEquals(listOf(15, 0, 0, 0, 0, 45, 30), week.days)
        assertEquals(90, week.total)
        assertEquals(3, week.activeDays)
        assertEquals("1 h 30 min this week, across 3 days", week.summary)
    }

    @Test
    fun aQuietWeekHasNothingToSayAndNothingToScold() {
        assertNull(FocusWeek.parse("", today = 50).summary)
        assertNull(FocusWeek.parse("30:60", today = 50).summary, "Last month is not this week")
    }

    @Test
    fun oneDayIsSaidAsOneDay() {
        assertEquals("25 min this week, on one day", FocusWeek.parse("50:25", today = 50).summary)
    }

    @Test
    fun recordingAddsToTheDayAndPrunesTheOld() {
        val stored = FocusWeek.record("80:10,100:20", day = 100, minutes = 15)
        assertEquals("100:35", stored, "Day 80 is older than the window and day 100 accumulates")
        assertEquals("100:35,101:5", FocusWeek.record(stored, 101, 5))
    }

    @Test
    fun garbageInStorageIsIgnoredNotFatal() {
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0), FocusWeek.parse("nonsense,1:x,:3", today = 9).days)
    }
}
