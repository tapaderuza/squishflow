package com.alvaropassalacqua.squishflow

import com.revenuecat.purchases.kmp.models.PackageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The plans screen's copy comes from the store's numbers; these hold it to the house rules. */
class PlansTest {

    @Test
    fun aYearlyPriceIsExplainedPerMonth() {
        assertEquals("$6.67", monthlyEquivalent("$79.98", 79_980_000L, 12))
        assertEquals("$6.67 a month, billed once", planLine(PackageType.ANNUAL, "$79.98", 79_980_000L).note)
    }

    @Test
    fun keepsTheCurrencyWhereTheStorePutIt() {
        assertEquals("6,67 €", monthlyEquivalent("79,98 €", 79_980_000L, 12))
        assertEquals("£0.83", monthlyEquivalent("£9.99", 9_990_000L, 12))
    }

    @Test
    fun roundsToTheNearestCent() {
        // 10.00 / 3 = 3.333.. -> 3.33; 20.00 / 3 = 6.666.. -> 6.67
        assertEquals("$3.33", monthlyEquivalent("$10.00", 10_000_000L, 3))
        assertEquals("$6.67", monthlyEquivalent("$20.00", 20_000_000L, 3))
    }

    @Test
    fun everyPlanLineKeepsTheHouseVoice() {
        PackageType.entries.forEach { type ->
            val line = planLine(type, "$9.99", 9_990_000L)
            assertTrue(!line.title.contains("!") && !line.note.contains("!"), "${line.title} shouts")
            assertTrue(line.note.length <= SquishyVoice.MAX_LENGTH, "${line.note} wraps")
            assertTrue(!line.note.contains("popular", ignoreCase = true) && !line.note.contains("best", ignoreCase = true),
                "No nudging badges on a plan row")
        }
    }

    @Test
    fun yearlyIsPreselectedWhenOffered() {
        assertEquals(1, defaultPlanIndex(listOf(PackageType.MONTHLY, PackageType.ANNUAL, PackageType.LIFETIME)))
        assertEquals(0, defaultPlanIndex(listOf(PackageType.MONTHLY, PackageType.LIFETIME)))
        assertEquals(0, defaultPlanIndex(emptyList()))
    }

    @Test
    fun anyActiveEntitlementIsPro() {
        assertTrue(RevenueCatManager.isPro(listOf("Squish Pro")))
        assertTrue(RevenueCatManager.isPro(listOf("squish_pro")), "The dashboard's spelling must not matter")
        assertTrue(!RevenueCatManager.isPro(emptyList()))
    }
}
