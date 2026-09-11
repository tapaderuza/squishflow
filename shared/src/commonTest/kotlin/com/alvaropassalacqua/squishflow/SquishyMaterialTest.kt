package com.alvaropassalacqua.squishflow

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FRAME = 1f / 60f

/** Frames a body takes to stop moving after one identical poke. */
private fun settleFrames(material: SquishyMaterial, limit: Int = 600): Int {
    val body = SquishyPhysics(tuning = material.tuning)
    body.impulse(angleRadians = 0f, strength = 7f)
    var frames = 0
    while (frames < limit && !body.isAtRest()) {
        body.advance(FRAME)
        frames++
    }
    return frames
}

/** Deepest deformation reached during that same poke. */
private fun peakTravel(material: SquishyMaterial): Float {
    val body = SquishyPhysics(tuning = material.tuning)
    body.impulse(angleRadians = 0f, strength = 7f)
    var peak = 0f
    repeat(240) {
        body.advance(FRAME)
        for (i in 0 until body.pointCount) peak = maxOf(peak, abs(body.displacementAt(i)))
    }
    return peak
}

class SquishyMaterialTest {

    @Test
    fun materialsAreNotReskins() {
        // The point of the Pro shelf is that the body answers differently. If two
        // materials settled at the same rate, the entitlement would be selling paint.
        val stressBall = settleFrames(SquishyMaterial.STRESS_BALL)
        val waterBalloon = settleFrames(SquishyMaterial.WATER_BALLOON)
        val jelly = settleFrames(SquishyMaterial.JELLY)

        assertTrue(
            stressBall < jelly,
            "Dense foam should recover faster than jelly: $stressBall vs $jelly frames",
        )
        assertTrue(
            waterBalloon > jelly,
            "A water balloon should keep sloshing: $waterBalloon vs $jelly frames",
        )
    }

    @Test
    fun stiffMaterialsTravelLessThanFloppyOnes() {
        val stressBall = peakTravel(SquishyMaterial.STRESS_BALL)
        val waterBalloon = peakTravel(SquishyMaterial.WATER_BALLOON)

        assertTrue(
            stressBall < waterBalloon,
            "Foam should resist the same poke a balloon gives in to: $stressBall vs $waterBalloon",
        )
    }

    @Test
    fun everyMaterialEventuallySettles() {
        SquishyMaterial.entries.forEach { material ->
            val frames = settleFrames(material)
            assertTrue(
                frames < 600,
                "${material.displayName} never came to rest within 10 seconds",
            )
        }
    }

    @Test
    fun everyMaterialStaysWithinItsOwnStretchLimit() {
        SquishyMaterial.entries.forEach { material ->
            val peak = peakTravel(material)
            assertTrue(
                peak <= material.tuning.maxDisplacement + 1e-4f,
                "${material.displayName} stretched to $peak past ${material.tuning.maxDisplacement}",
            )
        }
    }

    @Test
    fun exactlyOneMaterialIsFree() {
        val free = SquishyMaterial.entries.filter { !it.isPro }
        assertEquals(1, free.size, "Expected a single free body, got $free")
        assertEquals(SquishyMaterial.free, free.single())
    }

    @Test
    fun retuningKeepsTheCurrentShape() {
        val body = SquishyPhysics()
        body.press(angleRadians = 0f, depth = 0.3f)
        body.advance(FRAME)
        val before = body.displacementAt(0)

        body.retune(SquishyMaterial.STRESS_BALL.tuning)

        assertEquals(
            before,
            body.displacementAt(0),
            "Swapping material mid-gesture must not reset the body",
        )
    }

    @Test
    fun unknownOrMissingKeysFallBackToTheFreeBody() {
        assertEquals(SquishyMaterial.free, SquishyMaterial.fromKey(null))
        assertEquals(SquishyMaterial.free, SquishyMaterial.fromKey(""))
        assertEquals(SquishyMaterial.free, SquishyMaterial.fromKey("GLITTER_SLIME"))
    }

    @Test
    fun storedKeysRoundTrip() {
        SquishyMaterial.entries.forEach { material ->
            assertEquals(material, SquishyMaterial.fromKey(material.name))
        }
    }
}

class SettlingTest {

    private fun settleFramesFor(tuning: SquishyMaterial.Tuning, limit: Int = 900): Int {
        val body = SquishyPhysics(tuning = tuning)
        body.impulse(angleRadians = 0f, strength = 7f)
        var frames = 0
        while (frames < limit && !body.isAtRest()) {
            body.advance(1f / 60f)
            frames++
        }
        return frames
    }

    @Test
    fun theStartOfABlockChangesNothing() {
        val base = SquishyMaterial.JELLY.tuning
        assertEquals(base, base.settled(0f))
    }

    @Test
    fun aSettledBodyIsSofterAndCalmer() {
        val base = SquishyMaterial.JELLY.tuning
        val end = base.settled(1f)

        assertTrue(end.stiffness < base.stiffness, "A settled body should give more easily")
        assertTrue(end.damping > base.damping, "and should stop sooner, not wobble more")
    }

    @Test
    fun settlingReadsAsCalmRatherThanFloppy() {
        // Dropping stiffness alone would make the body wobble *longer* by the end
        // of a block, which is the opposite of the feeling being aimed at.
        val base = SquishyMaterial.JELLY.tuning
        assertTrue(
            settleFramesFor(base.settled(1f)) < settleFramesFor(base),
            "The end of a block should come to rest faster than the start",
        )
    }

    @Test
    fun theChangeIsMonotonic() {
        // Anything non-monotonic would read as the body changing its mind.
        val base = SquishyMaterial.STRESS_BALL.tuning
        val steps = (0..10).map { base.settled(it / 10f).stiffness }
        assertEquals(steps.sortedDescending(), steps, "Stiffness wandered: $steps")
    }

    @Test
    fun everyMaterialStaysStableWhileSettled() {
        SquishyMaterial.entries.forEach { material ->
            val frames = settleFramesFor(material.tuning.settled(1f))
            assertTrue(frames < 900, "${material.displayName} never rests when settled")
        }
    }

    @Test
    fun outOfRangeProgressIsClamped() {
        val base = SquishyMaterial.JELLY.tuning
        assertEquals(base.settled(1f), base.settled(4f))
        assertEquals(base.settled(0f), base.settled(-2f))
    }
}
