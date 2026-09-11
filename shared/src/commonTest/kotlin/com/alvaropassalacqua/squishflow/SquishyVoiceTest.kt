package com.alvaropassalacqua.squishflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Copy the app is contractually obliged to keep. These are the rules from
 * PRODUCT_DECISIONS.md made enforceable, so a future line cannot quietly break
 * the tone the product is built on.
 */
class SquishyVoiceTest {

    private fun everyLine(): List<String> = buildList {
        listOf(SquishyState.TENSE, SquishyState.RELAXING, SquishyState.COMPRESSED).forEach { state ->
            listOf(true, false).forEach { justCompleted ->
                (0..8).forEach { blocks ->
                    add(SquishyVoice.line(state, justCompleted, blocks))
                    add(SquishyVoice.line(state, justCompleted, blocks, minutesToNextBody = 12))
                }
            }
        }
    }

    @Test
    fun neverSaysNothing() {
        everyLine().forEach { assertTrue(it.isNotBlank(), "Produced an empty line") }
    }

    @Test
    fun everyLineFitsOnOneLine() {
        everyLine().forEach {
            assertTrue(
                it.length <= SquishyVoice.MAX_LENGTH,
                "\"$it\" is ${it.length} chars and will wrap, fighting the companion for attention",
            )
        }
    }

    @Test
    fun neverShouts() {
        // Enthusiasm raises the cost of starting, which is the opposite of the job.
        everyLine().forEach {
            assertTrue(!it.contains("!"), "\"$it\" uses an exclamation mark")
        }
    }

    @Test
    fun neverImpliesFailure() {
        val forbidden = listOf("fail", "lost", "broke", "streak", "again?", "missed")
        everyLine().forEach { line ->
            forbidden.forEach { word ->
                assertTrue(
                    !line.contains(word, ignoreCase = true),
                    "\"$line\" contains \"$word\"; an abandoned block is a thing that happened, not a verdict",
                )
            }
        }
    }

    @Test
    fun anInterruptionIsForgivenRatherThanMarked() {
        val line = SquishyVoice.line(SquishyState.COMPRESSED, justCompleted = false, completedBlocks = 3)
        assertTrue(
            line.contains("guilt") || line.contains("happens") ||
                line.contains("ended, not") || line.contains("Pick it up"),
            "Expected forgiveness, got \"$line\"",
        )
    }

    @Test
    fun finishingIsNotMistakenForReturning() {
        // The old copy said "You came back. That counts." after a *completed*
        // block, which is what you say to somebody who was interrupted.
        val line = SquishyVoice.line(SquishyState.TENSE, justCompleted = true, completedBlocks = 4)
        assertTrue(!line.contains("came back"), "Finishing a block is not coming back: \"$line\"")
    }

    @Test
    fun theFirstBlockGetsItsOwnWelcome() {
        val line = SquishyVoice.line(SquishyState.TENSE, justCompleted = false, completedBlocks = 0)
        assertEquals("First block. Start smaller than you think.", line)
    }

    @Test
    fun beingCloseToASquishyOutranksTheIdleInvitation() {
        val close = SquishyVoice.line(
            SquishyState.TENSE, justCompleted = false, completedBlocks = 5, minutesToNextBody = 12,
        )
        assertEquals("12 more minutes to the next squishy.", close)

        val faraway = SquishyVoice.line(
            SquishyState.TENSE, justCompleted = false, completedBlocks = 5, minutesToNextBody = 400,
        )
        assertTrue(!faraway.contains("more minutes"), "400 minutes away is not anticipation")
    }

    @Test
    fun anActiveBlockIsNeverInterrupted() {
        // Mid-session the app has one job: not be interesting.
        (0..6).forEach { blocks ->
            val line = SquishyVoice.line(
                SquishyState.RELAXING, justCompleted = false, completedBlocks = blocks,
                minutesToNextBody = 3,
            )
            assertTrue(!line.contains("squishy"), "Dangled a reward mid-block: \"$line\"")
        }
    }

    @Test
    fun linesRotateSoTheyDoNotGoStale() {
        val seen = (0..3).map {
            SquishyVoice.line(SquishyState.TENSE, justCompleted = false, completedBlocks = it + 1)
        }.toSet()
        assertTrue(seen.size > 1, "The same sentence forever: $seen")
    }

    @Test
    fun theSameMomentAlwaysReadsTheSame() {
        // Rotation has to stay a pure function of state, or the line would change
        // under somebody mid-glance on any recomposition.
        repeat(5) {
            assertEquals(
                SquishyVoice.line(SquishyState.TENSE, justCompleted = false, completedBlocks = 7),
                SquishyVoice.line(SquishyState.TENSE, justCompleted = false, completedBlocks = 7),
            )
        }
    }
}
