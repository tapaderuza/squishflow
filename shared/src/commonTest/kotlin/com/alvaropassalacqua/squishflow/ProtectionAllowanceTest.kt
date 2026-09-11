package com.alvaropassalacqua.squishflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The one Pro benefit that is not cosmetic, held in product logic rather than in copy. */
class ProtectionAllowanceTest {

    @Test
    fun freeProtectsAFewAppsAndThenOffersPro() {
        assertEquals(FREE_PROTECTED_APPS, protectedAppAllowance(isPremium = false))
        assertTrue(canProtectAnother(selectedCount = FREE_PROTECTED_APPS - 1, isPremium = false))
        assertFalse(canProtectAnother(selectedCount = FREE_PROTECTED_APPS, isPremium = false))
    }

    @Test
    fun proHasNoLimit() {
        assertEquals(null, protectedAppAllowance(isPremium = true))
        assertTrue(canProtectAnother(selectedCount = 40, isPremium = true))
    }

    @Test
    fun theFreeLimitIsEnoughToProveTheMechanic() {
        // One app would make the first setup feel like a demo; the number has to
        // cover the usual suspects before anyone is asked for money.
        assertTrue(FREE_PROTECTED_APPS >= 3)
    }
}
