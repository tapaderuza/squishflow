package com.alvaropassalacqua.squishflow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * The interactive body.
 *
 * Rendering is driven by [SquishyPhysics] rather than by `graphicsLayer` scaling:
 * the finger opens a dent exactly where it touches and the rest of the surface
 * answers, which is the difference between a mascot and something that feels
 * physical. The frame loop only runs while the body is moving, so an idle Squishy
 * costs nothing.
 */
@Composable
internal fun SquishyStage(
    state: SquishyState,
    progress: Float,
    accent: Color,
    onSquish: () -> Unit,
    modifier: Modifier = Modifier,
    material: SquishyMaterial = SquishyMaterial.free,
    reducedMotion: Boolean = false,
    stageSize: androidx.compose.ui.unit.Dp = 292.dp,
) {
    val body = remember { SquishyPhysics(tuning = material.tuning) }
    val mood = rememberFaceMood(reducedMotion)
    val haptics = rememberHaptics()
    val audio = rememberSquishAudio()
    val scope = rememberCoroutineScope()

    // Bumping this on every simulated frame is what re-runs the Canvas. Reading a
    // plain field of the physics object would not, because it is not snapshot state.
    var frame by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }

    var fingerAngle by remember { mutableStateOf(0f) }
    var fingerDepth by remember { mutableStateOf(0f) }
    var fingerDown by remember { mutableStateOf(false) }

    val driftX = remember { Animatable(0f) }
    val driftY = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val delta = ((now - previous) / 1_000_000_000.0).toFloat()
            previous = now
            // A tighter arc than the default reads as a fingertip pushing in,
            // rather than a whole hand flattening one side of the body.
            if (fingerDown) body.press(fingerAngle, fingerDepth, spread = 0.6f)
            body.advance(delta)
            frame++
            if (!fingerDown && body.isAtRest()) {
                running = false
                return@LaunchedEffect
            }
        }
    }

    // Switching material keeps the current deformation and lets the new constants
    // carry it home, then rings the body once so the new feel is immediately obvious.
    LaunchedEffect(material) {
        body.retune(material.tuning)
        if (!reducedMotion) {
            body.impulse(angleRadians = 0f, strength = 3.4f, spread = 1.3f)
            running = true
        }
    }

    // A completed block, or an interruption, should be felt on the body itself.
    LaunchedEffect(state) {
        if (reducedMotion) return@LaunchedEffect
        body.pulse(if (state == SquishyState.COMPRESSED) 3.6f else 2.2f)
        running = true
    }

    fun wake() {
        running = true
    }

    fun scatter() {
        scope.launch {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(720))
        }
    }

    val pressure = remember(frame) { body.pressure() }

    Box(
        modifier = modifier.size(stageSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color = Ink.copy(alpha = 0.055f), style = Stroke(width = 7.dp.toPx()))
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
            )
            if (burst.value < 1f) drawBurst(burst.value, accent)
        }

        Box(
            modifier = Modifier
                .size(250.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Squishy, your focus companion. Press and drag to relax it."
                    stateDescription = when (state) {
                        SquishyState.TENSE -> "Tense, ready to start"
                        SquishyState.RELAXING -> "Settling into the session"
                        SquishyState.COMPRESSED -> "Compressed after an interruption"
                    }
                }
                .pointerInput(reducedMotion) {
                    detectTapGestures(
                        onPress = {
                            haptics.play(HapticAccent.TOUCH)
                            audio.play(SquishGesture.SQUEEZE, intensity = 0.7f)
                            val centre = Offset(size.width / 2f, size.height / 2f)
                            body.impulse(
                                angleRadians = touchAngle(it.x - centre.x, it.y - centre.y),
                                strength = 5.5f,
                                spread = 0.55f,
                            )
                            wake()
                        },
                        onTap = {
                            haptics.play(HapticAccent.RELEASE)
                            audio.play(SquishGesture.RELEASE, intensity = 0.62f)
                            scatter()
                            onSquish()
                        },
                    )
                }
                .pointerInput(reducedMotion) {
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    val reach = min(size.width, size.height) / 2f
                    detectDragGestures(
                        onDragStart = { position ->
                            fingerDown = true
                            fingerAngle = touchAngle(position.x - centre.x, position.y - centre.y)
                            fingerDepth = depthFor(position, centre, reach)
                            haptics.play(HapticAccent.TOUCH)
                            audio.play(SquishGesture.SQUEEZE)
                            scatter()
                            wake()
                        },
                        onDragEnd = {
                            fingerDown = false
                            // Releasing a compressed surface snaps it outward.
                            body.impulse(fingerAngle, strength = -4.2f, spread = 1.1f)
                            haptics.play(HapticAccent.RELEASE)
                            audio.play(
                                SquishGesture.RELEASE,
                                intensity = (0.45f + fingerDepth * 1.4f).coerceAtMost(1f),
                            )
                            scope.launch {
                                launch { driftX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                launch { driftY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                            }
                            onSquish()
                        },
                        onDragCancel = {
                            fingerDown = false
                            scope.launch {
                                launch { driftX.animateTo(0f) }
                                launch { driftY.animateTo(0f) }
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        fingerAngle = touchAngle(change.position.x - centre.x, change.position.y - centre.y)
                        fingerDepth = depthFor(change.position, centre, reach)
                        haptics.play(HapticAccent.DEFORM)
                        scope.launch {
                            driftX.snapTo((driftX.value + dragAmount.x * 0.34f).coerceIn(-64f, 64f))
                            driftY.snapTo((driftY.value + dragAmount.y * 0.34f).coerceIn(-52f, 52f))
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            SquishyBody(
                body = body,
                state = state,
                finish = material.finish,
                frame = frame,
                driftX = driftX.value,
                driftY = driftY.value,
                reducedMotion = reducedMotion,
                modifier = Modifier.fillMaxSize(),
            )
            SquishyFace(
                state = state,
                lookX = driftX.value,
                lookY = driftY.value,
                pressure = pressure,
                mood = mood,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * How hard the surface is being pushed.
 *
 * A finger near the rim barely displaces anything; one dragged towards the centre
 * pushes the wall in deeply. Squaring the ratio makes the last stretch of travel
 * carry most of the deformation, which is how a real squishy resists.
 */
private fun depthFor(position: Offset, centre: Offset, reach: Float): Float {
    if (reach <= 0f) return 0f
    val distance = hypot(position.x - centre.x, position.y - centre.y)
    val penetration = ((reach - distance) / reach).coerceIn(0f, 1f)
    return 0.08f + penetration * penetration * 0.46f
}

/** Sub-surface flecks, scattered off-centre so they never read as a pattern. */
private val SPECKLES = listOf(
    0.29f to 0.70f, 0.68f to 0.26f, 0.73f to 0.58f,
    0.39f to 0.28f, 0.52f to 0.78f, 0.24f to 0.45f,
)

private fun DrawScope.drawBurst(phase: Float, accent: Color) {
    val directions = listOf(
        -1.0f to -0.25f, -0.72f to -0.72f, -0.2f to -1f,
        0.35f to -0.92f, 0.82f to -0.55f, 1f to 0.08f,
        0.72f to 0.72f, 0.12f to 1f, -0.55f to 0.82f,
        -0.92f to 0.45f,
    )
    directions.forEachIndexed { index, direction ->
        val distance = (78f + index * 4f) * phase * density
        drawCircle(
            color = (if (index % 3 == 0) Coral else accent).copy(alpha = (1f - phase) * 0.9f),
            radius = (3.5f + (index % 3) * 1.4f) * density * (1f - phase * 0.35f),
            center = center + Offset(direction.first * distance, direction.second * distance),
        )
    }
}

@Composable
private fun SquishyBody(
    body: SquishyPhysics,
    state: SquishyState,
    finish: SquishyMaterial.Finish,
    frame: Int,
    driftX: Float,
    driftY: Float,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val breathTransition = rememberInfiniteTransition(label = "squishy-breath")
    val breath by breathTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == SquishyState.RELAXING) 1900 else 2800,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )
    val tint = state.bodyTint()
    val shade = state.bodyShadow()

    Canvas(modifier) {
        @Suppress("UNUSED_EXPRESSION")
        frame // read the tick so the physics step invalidates this draw

        val w = size.width
        val h = size.height
        val pulse = if (reducedMotion) 1f else breath
        val compressed = if (state == SquishyState.COMPRESSED) 0.93f else 1f
        val radius = min(w, h) * 0.44f * pulse * compressed

        // Dragging stretches the whole body along the direction of travel while the
        // physics keeps denting it locally.
        val stretch = 1f + (abs(driftX) / 640f)
        val squashY = 1f - (abs(driftY) / 760f)

        val samples = body.sampleRing(squashX = stretch, squashY = squashY)
        val centre = Offset(w / 2f, h / 2f)
        val blob = samples.toBlobPath(centre, radius)

        drawOval(
            color = Color.Black.copy(alpha = 0.32f),
            topLeft = Offset(w * 0.19f, h * 0.83f),
            size = Size(w * 0.62f, h * 0.10f),
        )

        translate(left = driftX, top = driftY) {
            drawPath(
                path = blob,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.52f), tint, shade),
                    center = Offset(w * 0.34f, h * 0.25f),
                    radius = w * 0.78f,
                ),
            )
            drawPath(
                path = blob,
                color = Color.White.copy(alpha = finish.rim),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            clipPath(blob) {
                drawOval(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = finish.sheen), Color.Transparent),
                    ),
                    topLeft = Offset(w * 0.10f, h * 0.62f),
                    size = Size(w * 0.82f, h * 0.25f),
                )
                SPECKLES.take(finish.highlights).forEachIndexed { index, point ->
                    drawCircle(
                        color = Color.White.copy(alpha = finish.speckle + index * 0.012f),
                        radius = (5f + index * 1.7f) * density,
                        center = Offset(w * point.first, h * point.second),
                    )
                }
                drawOval(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = finish.gloss), Color.Transparent),
                    ),
                    topLeft = Offset(w * 0.24f, h * 0.16f),
                    size = Size(w * 0.27f, h * 0.12f),
                )
            }
        }
    }
}

/**
 * Turn the ring samples into a closed curve.
 *
 * Catmull-Rom converted to cubic Béziers: the curve passes exactly through every
 * simulated sample, so what the physics computes is what the eye sees. Joining the
 * points with straight lines would show facets as soon as the body deforms.
 */
private fun List<RingSample>.toBlobPath(centre: Offset, radius: Float): Path {
    val n = size
    val points = List(n) { Offset(centre.x + this[it].x * radius, centre.y + this[it].y * radius) }
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until n) {
            val p0 = points[(i - 1 + n) % n]
            val p1 = points[i]
            val p2 = points[(i + 1) % n]
            val p3 = points[(i + 2) % n]
            cubicTo(
                p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                p2.x, p2.y,
            )
        }
        close()
    }
}
