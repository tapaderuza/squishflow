package com.alvaropassalacqua.squishflow

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MissionPlannerTest {

    private val planner = LocalMissionPlanner()

    @Test
    fun neverRepeatsTheSameBlockTitle() = runTest {
        // The previous planner emitted "Prepare · <goal>" and "Finish · <goal>" for
        // every task, so a plan showed the same long sentence four times over.
        val plan = planner.plan("Finish the client deck and study for two hours")
        val focusTitles = plan.blocks.filter { it.kind == BlockKind.FOCUS }.map { it.title }

        assertEquals(
            focusTitles.size,
            focusTitles.toSet().size,
            "Duplicate block titles make a plan look broken: $focusTitles",
        )
    }

    @Test
    fun splitsOnConnectivesInEnglishAndSpanish() = runTest {
        val english = planner.plan("Write the report and review the budget")
        val spanish = planner.plan("Escribir el informe y revisar el presupuesto")

        listOf(english, spanish).forEach { plan ->
            val titles = plan.blocks.filter { it.kind == BlockKind.FOCUS }.map { it.title }
            assertTrue(
                titles.any { it.contains("report", ignoreCase = true) || it.contains("informe", true) },
                "Lost the first task in $titles",
            )
            assertTrue(
                titles.any { it.contains("budget", ignoreCase = true) || it.contains("presupuesto", true) },
                "Lost the second task in $titles",
            )
        }
    }

    @Test
    fun opensWithAShorterBlockThanRequested() = runTest {
        val plan = planner.plan("Rewrite the dissertation introduction", preferredMinutes = 45)
        val first = plan.blocks.first()

        assertTrue(first.minutes <= 15, "Starting is the expensive part, got ${first.minutes}m")
        assertTrue(
            plan.blocks.filter { it.kind == BlockKind.FOCUS }.drop(1).all { it.minutes == 45 },
            "Only the opening block should be shortened",
        )
    }

    @Test
    fun staysShortEnoughToBeBelievable() = runTest {
        val plan = planner.plan(
            "Finish the deck, study statistics, answer emails, fix the bug, call the bank, do laundry",
        )
        val focusCount = plan.blocks.count { it.kind == BlockKind.FOCUS }

        assertTrue(focusCount <= 6, "A plan longer than six blocks is a to-do list, got $focusCount")
    }

    @Test
    fun putsBreaksBetweenBlocksButNeverAtTheEnd() = runTest {
        val plan = planner.plan("Finish the client deck and study for two hours")

        assertTrue(plan.blocks.any { it.kind == BlockKind.BREAK }, "Expected at least one break")
        assertEquals(
            BlockKind.FOCUS,
            plan.blocks.last().kind,
            "A plan that ends on a break has nothing to finish",
        )
    }

    @Test
    fun clampsBlockLengthToHumanBounds() = runTest {
        val tooLong = planner.plan("Read the whole textbook", preferredMinutes = 240)
        assertTrue(tooLong.blocks.filter { it.kind == BlockKind.FOCUS }.all { it.minutes <= 60 })

        val tooShort = planner.plan("Read the whole textbook", preferredMinutes = 1)
        assertTrue(tooShort.blocks.filter { it.kind == BlockKind.FOCUS }.all { it.minutes >= 10 })
    }

    @Test
    fun stillRejectsInputTooVagueToPlan() = runTest {
        assertFailsWith<IllegalArgumentException> { planner.plan("do it") }
    }

    @Test
    fun titlesAreTrimmedOnWordBoundaries() {
        val long = "Finish the quarterly client presentation for the board meeting"
        val short = long.shorten(32)

        assertTrue(short.length <= 32, "Got ${short.length} chars: $short")
        assertTrue(long.startsWith(short), "Shortening must not rewrite the text")
        assertTrue(!short.endsWith(" "), "Trailing space in $short")
        assertTrue(
            long[short.length] == ' ' || long.length == short.length,
            "Cut mid-word: $short",
        )
    }

    @Test
    fun shortTitlesAreLeftAlone() {
        assertEquals("Study", "Study".shorten(32))
    }
}
