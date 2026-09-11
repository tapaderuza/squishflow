package com.alvaropassalacqua.squishflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The upgrade screen opens with the sentence the moment deserves, in the house voice. */
class PitchTest {

    @Test
    fun aTrialLeadsWithTheBodyThatJustLeftTheHand() {
        val copy = pitchCopy(PitchReason.Trial(SquishyMaterial.WATER_BALLOON, minutesToEarn = 209))
        assertEquals("Keep the water balloon.", copy.title)
        assertTrue("3 h 29 min" in copy.subtitle, copy.subtitle)
    }

    @Test
    fun theProtectionLimitLeadsWithProtection() {
        assertEquals("Protect every app.", pitchCopy(PitchReason.ProtectionLimit).title)
    }

    @Test
    fun everyPitchKeepsTheHouseVoice() {
        val reasons = listOf(
            PitchReason.Trial(SquishyMaterial.BUBBLE, 0),
            PitchReason.ProtectionLimit, PitchReason.Shelf, PitchReason.Introduction,
        )
        reasons.forEach { reason ->
            val copy = pitchCopy(reason)
            assertTrue(!copy.title.contains("!") && !copy.subtitle.contains("!"), "${copy.title} shouts")
            assertTrue(!copy.subtitle.contains("limited", true) && !copy.subtitle.contains("only today", true), "No urgency devices")
        }
    }

    @Test
    fun minutesReadLikeAPersonSaysThem() {
        assertEquals("45 min", formatMinutes(45))
        assertEquals("3 h", formatMinutes(180))
        assertEquals("3 h 29 min", formatMinutes(209))
    }
}
