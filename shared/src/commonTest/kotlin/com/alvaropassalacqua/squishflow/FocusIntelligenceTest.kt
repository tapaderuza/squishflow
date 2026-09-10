package com.alvaropassalacqua.squishflow

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FocusIntelligenceTest {
    @Test fun plannerCreatesFocusBlocksAndBreaks() = runTest {
        val plan = LocalMissionPlanner().plan("Terminar la presentación y estudiar el examen")
        assertTrue(plan.blocks.count { it.kind == BlockKind.FOCUS } >= 2)
        assertTrue(plan.blocks.any { it.kind == BlockKind.BREAK })
    }

    @Test fun plannerRejectsVagueInput() = runTest {
        assertFailsWith<IllegalArgumentException> { LocalMissionPlanner().plan("hacer") }
    }

    @Test fun coachAdaptsWithinSafeBounds() {
        assertEquals(30, AdaptiveFocusCoach.nextMinutes(25, SessionFeeling.EASY))
        assertEquals(20, AdaptiveFocusCoach.nextMinutes(25, SessionFeeling.TOO_MUCH))
        assertEquals(10, AdaptiveFocusCoach.nextMinutes(10, SessionFeeling.TOO_MUCH))
        assertEquals(60, AdaptiveFocusCoach.nextMinutes(60, SessionFeeling.EASY))
    }

    @Test fun missionRoundTripsAcrossProcessPersistence() {
        val original = FocusMission(
            title = "Presentación ñ",
            originalGoal = "Terminar cliente y estudiar",
            blocks = listOf(
                MissionBlock("Investigar", 25),
                MissionBlock("Respirar", 10, BlockKind.BREAK),
            ),
        )
        val restored = deserializeMission(original.serialize(index = 1, accepted = true))!!
        assertEquals(original, restored.mission)
        assertEquals(1, restored.index)
        assertTrue(restored.accepted)
    }
}
