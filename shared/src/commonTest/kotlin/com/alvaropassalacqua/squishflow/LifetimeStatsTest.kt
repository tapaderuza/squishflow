package com.alvaropassalacqua.squishflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LifetimeStatsTest {

    @Test
    fun consistencyIsUnknownBeforeTheFirstAttempt() {
        // Showing 0% to somebody who has not started reads as a judgement they
        // have not earned.
        assertNull(LifetimeStats(focusedMinutes = 0, completedBlocks = 0, failedBlocks = 0).consistency)
    }

    @Test
    fun consistencyIsTheShareOfStartedBlocksThatFinished() {
        val stats = LifetimeStats(focusedMinutes = 235, completedBlocks = 14, failedBlocks = 3)

        assertEquals(17, stats.attempts)
        assertEquals(82, stats.consistency)
    }

    @Test
    fun aPerfectRecordReadsAsAHundred() {
        val stats = LifetimeStats(focusedMinutes = 60, completedBlocks = 4, failedBlocks = 0)
        assertEquals(100, stats.consistency)
    }

    @Test
    fun abandoningEverythingIsStillReportedHonestly() {
        val stats = LifetimeStats(focusedMinutes = 0, completedBlocks = 0, failedBlocks = 5)
        assertEquals(0, stats.consistency)
    }

    @Test
    fun compactRemainingFitsANarrowColumn() {
        assertEquals("Ready", shortRemaining(0))
        assertEquals("45 min", shortRemaining(45))
        assertEquals("3 h", shortRemaining(180))
        assertEquals("3h 5m", shortRemaining(185))
    }
}

class DaysAwayTest {
    @Test fun neverOpenedIsNotAnAbsence() = assertEquals(null, daysAway(null, 20_000L))
    @Test fun sameDayIsZero() = assertEquals(0, daysAway(20_000L, 20_000L))
    @Test fun countsCalendarDays() = assertEquals(4, daysAway(20_000L, 20_004L))
    @Test fun aClockSetBackwardsIsNotANegativeAbsence() = assertEquals(0, daysAway(20_010L, 20_000L))
}
