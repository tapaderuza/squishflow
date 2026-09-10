package com.alvaropassalacqua.squishflow

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private const val TAU = (2.0 * PI).toFloat()

/**
 * Soft-body model for Squishy.
 *
 * The silhouette is a ring of [pointCount] radial samples. Each sample holds a
 * displacement and a velocity and is coupled to its neighbours, so a press at one
 * angle opens a dent that travels around the body as a damped wave instead of
 * scaling the whole sprite uniformly.
 *
 * Two properties make it read as jelly rather than as an animation:
 *
 * - **Neighbour coupling** spreads each impulse around the ring, which is the
 *   discrete wave equation and is what gives the surface its wobble.
 * - **Volume conservation** removes the mean displacement every step, so pressing
 *   in on one side necessarily bulges the other. Without it the body just shrinks.
 *
 * Pure Kotlin with no Compose dependency: [advance] takes a real delta in seconds,
 * so the same gesture feels identical at 60, 90 and 120 Hz, and the whole model is
 * unit-testable.
 */
class SquishyPhysics(
    val pointCount: Int = 26,
    private val stiffness: Float = 118f,
    private val damping: Float = 7.1f,
    private val coupling: Float = 46f,
) {
    init {
        require(pointCount >= 8) { "A ring needs at least 8 samples to look round" }
        require(stiffness > 0f && damping > 0f)
    }

    private val displacement = FloatArray(pointCount)
    private val velocity = FloatArray(pointCount)
    private val scratch = FloatArray(pointCount)

    /** Radial displacement at [index], as a fraction of the base radius. Negative is inward. */
    fun displacementAt(index: Int): Float = displacement[index.mod(pointCount)]

    /** Angle of sample [index], measured clockwise from the top of the body. */
    fun angleAt(index: Int): Float = TAU * index / pointCount

    /** True once every sample has settled, so the render loop can stop asking for frames. */
    fun isAtRest(epsilon: Float = 0.0015f): Boolean {
        for (i in 0 until pointCount) {
            if (abs(displacement[i]) > epsilon || abs(velocity[i]) > epsilon * 12f) return false
        }
        return true
    }

    /**
     * Push the surface at [angleRadians].
     *
     * [strength] is the peak inward velocity as a fraction of the base radius per
     * second; negative values pull outward. [spread] is the angular standard
     * deviation of the affected arc in radians — small values poke, large values
     * squash a whole side.
     */
    fun impulse(angleRadians: Float, strength: Float, spread: Float = 0.9f) {
        val safeSpread = spread.coerceAtLeast(0.05f)
        val twoSigmaSquared = 2f * safeSpread * safeSpread
        for (i in 0 until pointCount) {
            val delta = angularDistance(angleAt(i), angleRadians)
            velocity[i] -= strength * exp(-(delta * delta) / twoSigmaSquared)
        }
    }

    /**
     * Hold the surface deformed at [angleRadians] while a finger rests on it.
     *
     * Unlike [impulse] this drives displacement directly towards [depth] rather than
     * adding velocity, so a stationary finger holds a stable dent instead of
     * pumping energy into the ring every frame.
     */
    fun press(angleRadians: Float, depth: Float, spread: Float = 0.9f, response: Float = 0.35f) {
        val safeSpread = spread.coerceAtLeast(0.05f)
        val twoSigmaSquared = 2f * safeSpread * safeSpread
        val blend = response.coerceIn(0f, 1f)
        for (i in 0 until pointCount) {
            val delta = angularDistance(angleAt(i), angleRadians)
            val weight = exp(-(delta * delta) / twoSigmaSquared)
            val target = -depth * weight
            displacement[i] += (target - displacement[i]) * blend * weight
        }
    }

    /** Ripple the whole ring, used when a focus block completes. */
    fun pulse(strength: Float) {
        for (i in 0 until pointCount) {
            velocity[i] -= strength * sin(angleAt(i) * 3f)
        }
    }

    /** Drop every deformation immediately, without animating back. */
    fun reset() {
        displacement.fill(0f)
        velocity.fill(0f)
    }

    /**
     * Integrate the ring forward by [deltaSeconds].
     *
     * Large deltas are split into fixed sub-steps: a dropped frame must not be able
     * to push the explicit integrator past its stability limit and blow the body up.
     */
    fun advance(deltaSeconds: Float) {
        if (deltaSeconds <= 0f) return
        var remaining = deltaSeconds.coerceAtMost(0.25f)
        while (remaining > 0f) {
            val step = if (remaining > MAX_STEP) MAX_STEP else remaining
            integrate(step)
            remaining -= step
        }
    }

    private fun integrate(dt: Float) {
        // Neighbour coupling: the discrete Laplacian around the ring.
        for (i in 0 until pointCount) {
            val previous = displacement[(i - 1).mod(pointCount)]
            val next = displacement[(i + 1).mod(pointCount)]
            scratch[i] = previous + next - 2f * displacement[i]
        }
        for (i in 0 until pointCount) {
            val acceleration = -stiffness * displacement[i] - damping * velocity[i] + coupling * scratch[i]
            velocity[i] += acceleration * dt
            displacement[i] += velocity[i] * dt
        }
        conserveVolume()
    }

    /**
     * Keep the mean radius constant so a dent on one side becomes a bulge on the
     * other. This is what stops the body from simply deflating under a long press.
     */
    private fun conserveVolume() {
        var sum = 0f
        for (i in 0 until pointCount) sum += displacement[i]
        val mean = sum / pointCount
        for (i in 0 until pointCount) {
            displacement[i] -= mean
            displacement[i] = displacement[i].coerceIn(-MAX_DISPLACEMENT, MAX_DISPLACEMENT)
        }
    }

    private companion object {
        const val MAX_STEP = 1f / 240f
        const val MAX_DISPLACEMENT = 0.42f
    }
}

/** Shortest signed distance between two angles, in radians. */
internal fun angularDistance(a: Float, b: Float): Float {
    var delta = (a - b) % TAU
    if (delta > PI) delta -= TAU
    if (delta < -PI) delta += TAU
    return abs(delta)
}

/**
 * Convert a touch offset measured from the centre of the body into the angle used
 * by [SquishyPhysics], measured clockwise from the top.
 */
internal fun touchAngle(dx: Float, dy: Float): Float {
    val angle = kotlin.math.atan2(dx, -dy)
    return if (angle < 0f) angle + TAU else angle
}

/** Unit-circle sample used by the renderer, already displaced by the physics. */
internal data class RingSample(val x: Float, val y: Float)

/**
 * Sample the deformed ring as unit-circle coordinates.
 *
 * [squashX] and [squashY] apply the whole-body stretch on top of the local
 * deformation, so a drag can elongate Squishy while a press still dents it.
 */
internal fun SquishyPhysics.sampleRing(
    squashX: Float = 1f,
    squashY: Float = 1f,
): List<RingSample> = List(pointCount) { i ->
    val angle = angleAt(i)
    val radius = 1f + displacementAt(i)
    RingSample(
        x = sin(angle) * radius * squashX,
        y = -cos(angle) * radius * squashY,
    )
}
