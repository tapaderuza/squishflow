package com.alvaropassalacqua.squishflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaterialAccessTest {

    @Test
    fun theFreeBodyIsAlwaysAvailable() {
        val access = SquishyMaterial.JELLY.accessWith(focusedMinutes = 0, isPremium = false)
        assertEquals(MaterialAccess.Free, access)
        assertTrue(access.isUsable)
    }

    @Test
    fun focusAloneUnlocksABody() {
        val material = SquishyMaterial.STRESS_BALL
        val before = material.accessWith(material.unlockMinutes - 1, isPremium = false)
        val after = material.accessWith(material.unlockMinutes, isPremium = false)

        assertTrue(before is MaterialAccess.Locked, "Should still be locked one minute short")
        assertEquals(MaterialAccess.Earned(material.unlockMinutes), after)
        assertTrue(after.isUsable, "Focus is a real currency or the model is a lie")
    }

    @Test
    fun proUnlocksEverythingImmediately() {
        SquishyMaterial.entries.forEach { material ->
            val access = material.accessWith(focusedMinutes = 0, isPremium = true)
            assertTrue(access.isUsable, "${material.displayName} should be open to a subscriber")
        }
    }

    @Test
    fun lapsingProKeepsWhatWasEarned() {
        // Taking back a body someone focused 60 minutes for would punish them for
        // having used the app exactly as intended.
        val material = SquishyMaterial.STRESS_BALL
        val access = material.accessWith(material.unlockMinutes, isPremium = false)

        assertEquals(MaterialAccess.Earned(material.unlockMinutes), access)
    }

    @Test
    fun lapsingProRemovesOnlyWhatWasNotEarned() {
        val material = SquishyMaterial.BUBBLE
        val access = material.accessWith(focusedMinutes = 30, isPremium = false)

        assertTrue(access is MaterialAccess.Locked)
        assertEquals(30, (access as MaterialAccess.Locked).focusedMinutes)
    }

    @Test
    fun lockedProgressReportsHowCloseYouAre() {
        val material = SquishyMaterial.STRESS_BALL
        val halfway = material.accessWith(material.unlockMinutes / 2, isPremium = false)

        assertTrue(halfway is MaterialAccess.Locked)
        val locked = halfway as MaterialAccess.Locked
        assertTrue(locked.progress in 0.45f..0.55f, "Expected about half, got ${locked.progress}")
        assertEquals(material.unlockMinutes / 2, locked.remainingMinutes)
    }

    @Test
    fun theLadderRisesInDeclarationOrder() {
        // The picker renders declaration order, so a ladder that jumped around
        // would read as arbitrary rather than as progress.
        val thresholds = SquishyMaterial.entries.map { it.unlockMinutes }
        assertEquals(thresholds.sorted(), thresholds, "Unlock ladder is out of order: $thresholds")
    }

    @Test
    fun theFirstRungIsReachableInADay() {
        val first = SquishyMaterial.entries.first { it.isPro }
        assertTrue(
            first.unlockMinutes <= 90,
            "The mechanic has to prove itself early, got ${first.unlockMinutes} min",
        )
    }

    @Test
    fun nextTargetIsTheNearestLockedBody() {
        assertEquals(SquishyMaterial.STRESS_BALL, nextMaterialToEarn(0))
        assertEquals(SquishyMaterial.MOCHI, nextMaterialToEarn(SquishyMaterial.STRESS_BALL.unlockMinutes))
        assertEquals(null, nextMaterialToEarn(100_000), "Everything should be open eventually")
    }

    @Test
    fun remainingTimeReadsLikeAPersonWroteIt() {
        assertEquals("ready", formatRemaining(0))
        assertEquals("45 min of focus away", formatRemaining(45))
        assertEquals("2 h of focus away", formatRemaining(120))
        assertEquals("1 h 30 min of focus away", formatRemaining(90))
    }
}
