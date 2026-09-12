package com.alvaropassalacqua.squishflow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

/**
 * What the face is doing that nobody asked it to.
 *
 * A companion that only moves when touched is a control, not a character. Two
 * autonomous behaviours do almost all the work of making it read as alive:
 *
 * - **Blinking.** A face that never blinks is uncanny, and this is the cheapest
 *   possible fix for it.
 * - **Gaze drift.** Eyes that hold one position look painted on. Letting them
 *   wander a few pixels every few seconds reads as something thinking.
 *
 * Both are suppressed under reduced motion, because both are movement the person
 * did not ask for, which is the contract the rest of the app keeps.
 */
internal data class FaceMood(
    /** 1 is fully open, 0 fully closed. */
    val openness: Float = 1f,
    /** Where the eyes have wandered to, in pixels. */
    val gaze: Offset = Offset.Zero,
    /**
     * 0 to 1: eyes gently shut and the smile widens. The face a block earns
     * when it finishes — not a cheer, a sigh.
     */
    val contentment: Float = 0f,
)

/**
 * @param contented true on the screen a finished block lands on: the eyes
 * close for a moment and the smile widens, then the face comes back. Kept on a
 * separate animatable from blinking so the two never cancel each other.
 */
@Composable
internal fun rememberFaceMood(
    reducedMotion: Boolean,
    contented: Boolean = false,
    /** Late at night the lids sit lower; see [isLateNight]. */
    sleepy: Boolean = false,
): FaceMood {
    // Starts closed: the first thing the face does is open its eyes.
    val openness = remember { Animatable(0.04f) }
    val gazeX = remember { Animatable(0f) }
    val gazeY = remember { Animatable(0f) }
    val rest = remember { Animatable(0f) }
    val still by rememberUpdatedState(reducedMotion)

    LaunchedEffect(contented) {
        if (!contented) { rest.snapTo(0f); return@LaunchedEffect }
        // After the eyes have opened, not instead of it.
        delay(if (still) 0 else 1_100)
        rest.animateTo(1f, tween(if (still) 0 else 420, easing = FastOutSlowInEasing))
        delay(1_500)
        rest.animateTo(0f, tween(if (still) 0 else 560, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(reducedMotion) {
        if (still) {
            openness.snapTo(1f)
            return@LaunchedEffect
        }
        // Waking up: a beat of stillness, then the eyes open unhurriedly.
        delay(420)
        openness.animateTo(1f, tween(520, easing = FastOutSlowInEasing))

        val random = Random(0xB11)
        while (true) {
            // Human blink spacing is irregular. A fixed interval reads as a
            // metronome and is somehow worse than not blinking at all.
            delay(random.nextLong(2_600, 6_400))
            repeat(if (random.nextInt(100) < 18) 2 else 1) {
                openness.animateTo(0.06f, tween(70, easing = FastOutSlowInEasing))
                openness.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
            }
        }
    }

    LaunchedEffect(reducedMotion) {
        if (still) {
            gazeX.snapTo(0f)
            gazeY.snapTo(0f)
            return@LaunchedEffect
        }
        val random = Random(0x9A2)
        while (true) {
            delay(random.nextLong(1_900, 4_800))
            val targetX = random.nextFloat() * 6f - 3f
            val targetY = random.nextFloat() * 3f - 1.5f
            // Slow enough to read as attention moving, not as a twitch.
            gazeX.animateTo(targetX, tween(900, easing = LinearEasing))
            gazeY.animateTo(targetY, tween(900, easing = LinearEasing))
        }
    }

    return FaceMood(
        openness = openness.value * (1f - 0.9f * rest.value) * (if (sleepy) 0.78f else 1f),
        gaze = Offset(gazeX.value, gazeY.value),
        contentment = rest.value,
    )
}

/**
 * The face.
 *
 * [pressure] is how hard the body is currently being deformed, 0 to 1. The eyes
 * narrow with it, because a squishy being squeezed should look like it notices.
 */
@Composable
internal fun SquishyFace(
    state: SquishyState,
    lookX: Float,
    lookY: Float,
    pressure: Float,
    mood: FaceMood,
    modifier: Modifier = Modifier,
    temperament: SquishyMaterial.Temperament = SquishyMaterial.Temperament(),
) {
    Canvas(modifier) {
        val shift = (lookX / 8f).coerceIn(-7f, 7f)
        val verticalShift = (lookY / 10f).coerceIn(-4f, 4f)
        val eyeY = size.height * if (state == SquishyState.COMPRESSED) 0.47f else 0.43f
        val eyeGap = size.width * 0.105f * temperament.eyeGap
        val radius = (if (state == SquishyState.COMPRESSED) 5.dp.toPx() else 7.5.dp.toPx()) * temperament.eyeScale

        val centre = Offset(
            x = size.width / 2f + shift + lookX + mood.gaze.x,
            y = eyeY + verticalShift + lookY + mood.gaze.y,
        )
        val left = Offset(centre.x - eyeGap, centre.y)
        val right = Offset(centre.x + eyeGap, centre.y)

        if (state == SquishyState.COMPRESSED) {
            drawCross(left, radius)
            drawCross(right, radius)
        } else {
            // Blinking and squinting both close the eye, so they multiply rather
            // than fight: a blink during a hard squeeze still shuts completely.
            val squint = 1f - (pressure.coerceIn(0f, 1f) * 0.55f)
            val open = (mood.openness * squint * temperament.lids).coerceIn(0.04f, 1f)
            drawEye(left, radius, open)
            drawEye(right, radius, open)
        }

        val blushAlpha = (if (state == SquishyState.RELAXING) 0.42f else 0.25f) * temperament.blush
        drawCircle(
            Blush.copy(alpha = blushAlpha),
            radius = 10.dp.toPx(),
            center = Offset(size.width * 0.35f + lookX, size.height * 0.52f + lookY),
        )
        drawCircle(
            Blush.copy(alpha = blushAlpha),
            radius = 10.dp.toPx(),
            center = Offset(size.width * 0.65f + lookX, size.height * 0.52f + lookY),
        )

        // The mouth. A hard press purses it into a small "o": a squishy being
        // squeezed should look like it noticed there too, not only in the eyes.
        // Contentment widens the smile and lifts its corners.
        val purse = ((pressure - 0.35f) / 0.45f).coerceIn(0f, 1f)
        val wide = (1f + 0.35f * mood.contentment) * temperament.smile
        val mouthWidth = size.width * 0.14f * (1f - 0.55f * purse) * wide
        val mouthHeight = size.height * (0.09f + 0.03f * mood.contentment) * (1f + 0.6f * purse)
        val mouthLeft = size.width / 2f - mouthWidth / 2f + lookX
        val mouthTop = size.height * 0.49f + lookY - size.height * 0.02f * mood.contentment
        if (state != SquishyState.COMPRESSED && purse > 0.6f) {
            drawOval(
                color = OnLight,
                topLeft = Offset(mouthLeft, mouthTop + mouthHeight * 0.25f),
                size = Size(mouthWidth, mouthHeight * 0.75f),
                style = Stroke(width = 3.5.dp.toPx()),
            )
        } else {
            drawArc(
                color = OnLight,
                startAngle = if (state == SquishyState.COMPRESSED) 205f else 20f - 12f * mood.contentment,
                sweepAngle = ((if (state == SquishyState.RELAXING) 140f else 130f) + 24f * mood.contentment) * temperament.smile.coerceIn(0.8f, 1.1f),
                useCenter = false,
                topLeft = Offset(mouthLeft, mouthTop),
                size = Size(mouthWidth, mouthHeight),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * One eye, squashed vertically by [open].
 *
 * A closing eye keeps its width and loses its height, which is what an eyelid
 * actually does; scaling both would read as the eye shrinking away instead.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEye(
    centre: Offset,
    radius: Float,
    open: Float,
) {
    val height = radius * 2f * open
    drawOval(
        color = OnLight,
        topLeft = Offset(centre.x - radius, centre.y - height / 2f),
        size = Size(radius * 2f, height),
    )
    // The catchlight only survives while there is an eye to sit in.
    if (open > 0.45f) {
        drawCircle(
            Color.White.copy(alpha = 0.9f * ((open - 0.45f) / 0.55f)),
            radius * 0.28f,
            center = centre + Offset(-radius * 0.25f, -radius * 0.28f * open),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCross(centre: Offset, radius: Float) {
    val arm = radius * 0.95f
    drawLine(
        OnLight,
        start = centre + Offset(-arm, -arm * 0.6f),
        end = centre + Offset(arm, arm * 0.6f),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        OnLight,
        start = centre + Offset(-arm, arm * 0.6f),
        end = centre + Offset(arm, -arm * 0.6f),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

/** How deformed the body currently is, as a 0..1 value the face can react to. */
internal fun SquishyPhysics.pressure(): Float {
    var peak = 0f
    for (i in 0 until pointCount) peak = maxOf(peak, abs(displacementAt(i)))
    return (peak / 0.35f).coerceIn(0f, 1f)
}
