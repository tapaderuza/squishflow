package com.alvaropassalacqua.squishflow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private const val FRAME = 1f / 60f

private fun SquishyPhysics.advanceSeconds(seconds: Float) {
    var elapsed = 0f
    while (elapsed < seconds) {
        advance(FRAME)
        elapsed += FRAME
    }
}

private fun SquishyPhysics.maxDisplacement(): Float =
    (0 until pointCount).maxOf { abs(displacementAt(it)) }

class SquishyPhysicsTest {

    @Test
    fun restsFlatBeforeAnyTouch() {
        val body = SquishyPhysics()
        assertTrue(body.isAtRest(), "A body nobody touched must be perfectly round")
        assertTrue(body.maxDisplacement() == 0f)
    }

    @Test
    fun pressDentsTheTouchedSideAndBulgesTheOpposite() {
        val body = SquishyPhysics()
        val top = 0f
        body.press(angleRadians = top, depth = 0.3f)
        // press() only sets the target; volume is conserved on the integration step,
        // which is the same order the render loop uses.
        body.advance(FRAME)

        val dent = body.displacementAt(0)
        val opposite = body.displacementAt(body.pointCount / 2)

        assertTrue(dent < -0.05f, "The touched sample should move inward, was $dent")
        assertTrue(opposite > 0f, "Volume conservation should bulge the far side, was $opposite")
    }

    @Test
    fun deformationTravelsToNeighboursButNotInstantlyAcrossTheBody() {
        val body = SquishyPhysics()
        body.impulse(angleRadians = 0f, strength = 6f, spread = 0.35f)
        body.advance(FRAME)

        val neighbour = abs(body.displacementAt(1))
        val across = abs(body.displacementAt(body.pointCount / 2))

        assertTrue(neighbour > across, "Coupling should reach neighbours first: $neighbour vs $across")
    }

    @Test
    fun settlesBackToRoundAfterRelease() {
        val body = SquishyPhysics()
        body.impulse(angleRadians = 1.2f, strength = 8f)
        assertTrue(!body.isAtRest(), "The body should be moving right after an impulse")

        body.advanceSeconds(4f)

        assertTrue(body.isAtRest(), "A squishy that never settles reads as broken")
    }

    @Test
    fun survivesADroppedFrameWithoutExploding() {
        val body = SquishyPhysics()
        body.impulse(angleRadians = 0f, strength = 10f)
        // A 400 ms stall would push a naive explicit integrator past its stability limit.
        body.advance(0.4f)

        assertTrue(
            body.maxDisplacement() < 1f,
            "Long deltas must be sub-stepped, peak was ${body.maxDisplacement()}",
        )
    }

    @Test
    fun repeatedPressHoldsADentInsteadOfPumpingEnergy() {
        val body = SquishyPhysics()
        // A finger resting on the body for half a second, one call per frame.
        repeat(30) {
            body.press(angleRadians = 0f, depth = 0.25f)
            body.advance(FRAME)
        }

        assertTrue(
            body.maxDisplacement() < 0.45f,
            "A held finger must not resonate, peak was ${body.maxDisplacement()}",
        )
        assertTrue(body.displacementAt(0) < 0f, "The dent should still be under the finger")
    }

    @Test
    fun resetClearsEverything() {
        val body = SquishyPhysics()
        body.impulse(angleRadians = 2f, strength = 9f)
        body.advance(FRAME)
        body.reset()

        assertTrue(body.isAtRest())
    }

    @Test
    fun angularDistanceWrapsAroundTheSeam() {
        // 0.1 rad past the top and 0.1 rad before it are neighbours, not a full turn apart.
        val distance = angularDistance(0.1f, (2 * PI).toFloat() - 0.1f)
        assertTrue(abs(distance - 0.2f) < 1e-4f, "Expected ~0.2 rad, got $distance")
    }

    @Test
    fun touchAngleIsMeasuredClockwiseFromTheTop() {
        val tolerance = 1e-4f
        assertTrue(abs(touchAngle(0f, -1f) - 0f) < tolerance, "Straight up is zero")
        assertTrue(abs(touchAngle(1f, 0f) - (PI / 2).toFloat()) < tolerance, "Right is a quarter turn")
        assertTrue(abs(touchAngle(0f, 1f) - PI.toFloat()) < tolerance, "Down is a half turn")
    }

    @Test
    fun sampledRingStaysCenteredOnTheBody() {
        val body = SquishyPhysics()
        body.press(angleRadians = 0f, depth = 0.3f)
        val samples = body.sampleRing()

        val meanX = samples.sumOf { it.x.toDouble() } / samples.size
        val meanY = samples.sumOf { it.y.toDouble() } / samples.size

        assertTrue(abs(meanX) < 0.12 && abs(meanY) < 0.12, "Body drifted to ($meanX, $meanY)")
    }
}

class StretchTest {

    @Test
    fun aStretchElongatesVerticallyAndNarrowsTheSides() {
        val body = SquishyPhysics()
        body.stretch(2f)
        body.advance(1f / 60f)

        val top = body.displacementAt(0)
        val side = body.displacementAt(body.pointCount / 4)
        assertTrue(top > 0f, "Top should move outward, was $top")
        assertTrue(side < 0f, "Sides should move inward, was $side")
    }

    @Test
    fun aStretchIsNotARipple() {
        // Second mode versus third: a stretch must have exactly two lobes, or it
        // reads as excitement rather than as a sigh.
        val body = SquishyPhysics()
        body.stretch(2f)
        body.advance(1f / 60f)
        val n = body.pointCount
        val quarter = n / 4
        assertTrue(body.displacementAt(0) > 0f && body.displacementAt(n / 2) > 0f, "Both poles out")
        assertTrue(body.displacementAt(quarter) < 0f && body.displacementAt(3 * quarter) < 0f, "Both sides in")
    }

    @Test
    fun aStretchSettlesBackToRound() {
        val body = SquishyPhysics()
        body.stretch(2f)
        var frames = 0
        while (frames < 600 && !body.isAtRest()) { body.advance(1f / 60f); frames++ }
        assertTrue(body.isAtRest(), "A sigh that never ends is a tremor")
    }
}
