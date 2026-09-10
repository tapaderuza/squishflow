package com.alvaropassalacqua.squishflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MissionPlannerScreen(
    planner: MissionPlanner = remember { LocalMissionPlanner() },
    onMissionReady: (FocusMission) -> Unit,
) {
    var goal by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var planning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ink = PaperInk
    val paper = Paper
    val sage = Sage

    Surface(Modifier.fillMaxSize(), color = paper) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(26.dp),
        ) {
            Text("SQUISHFLOW · ADAPTIVE FOCUS", color = ink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
            Spacer(Modifier.weight(.7f))
            Text("What do you want\nto finish today?", color = ink, fontSize = 38.sp, lineHeight = 43.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(14.dp))
            Text(
                "Tell Squishy. It becomes blocks you can actually finish.",
                color = ink.copy(alpha = .58f), fontSize = 15.sp, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it.take(500); error = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                placeholder = { Text("e.g. Finish the client deck and study for two hours") },
                enabled = !planning,
                shape = RoundedCornerShape(22.dp),
                supportingText = { Text("${goal.length}/500") },
            )
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    planning = true
                    scope.launch {
                        runCatching { planner.plan(goal) }
                            .onSuccess(onMissionReady)
                            .onFailure { error = "Try a slightly more concrete goal." }
                        planning = false
                    }
                },
                enabled = goal.trim().length >= 8 && !planning,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = sage),
            ) {
                if (planning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = sage)
                else Text("Create my mission", fontWeight = FontWeight.Bold)
            }
            Text(
                "Your plan is built on this device.",
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = ink.copy(alpha = .42f), fontSize = 11.sp,
            )
        }
    }
}

@Composable
fun MissionReviewScreen(
    mission: FocusMission,
    onBack: () -> Unit,
    onDurationChanged: (Int, Int) -> Unit,
    onStart: () -> Unit,
) {
    val ink = PaperInk
    val paper = Paper
    val sage = Sage
    Surface(Modifier.fillMaxSize(), color = paper) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("← Change goal") }
            Spacer(Modifier.height(24.dp))
            Text("MISSION", color = sage, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(mission.title, color = ink, fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(26.dp))
            mission.blocks.forEachIndexed { index, block ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .background(ink.copy(alpha = if (block.kind == BlockKind.BREAK) .035f else .065f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text((index + 1).toString().padStart(2, '0'), color = ink.copy(alpha = .35f), fontSize = 11.sp)
                    Spacer(Modifier.width(14.dp))
                    Text(block.title, Modifier.weight(1f), color = ink, fontSize = 14.sp)
                    IconButton(
                        onClick = { onDurationChanged(index, (block.minutes - 5).coerceAtLeast(5)) },
                        modifier = Modifier.semantics {
                            contentDescription = "Shorten ${block.title}, currently ${block.minutes} minutes"
                        },
                    ) { Text("−") }
                    Text(
                        "${block.minutes}m",
                        color = if (block.kind == BlockKind.BREAK) ink.copy(alpha = .45f) else sage,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { onDurationChanged(index, (block.minutes + 5).coerceAtMost(60)) },
                        modifier = Modifier.semantics {
                            contentDescription = "Lengthen ${block.title}, currently ${block.minutes} minutes"
                        },
                    ) { Text("+") }
                }
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = sage),
            ) { Text("Start the first block", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun MissionBreakScreen(
    block: MissionBlock,
    nextBlock: MissionBlock?,
    material: SquishyMaterial,
    reducedMotion: Boolean,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    // The one screen on the main path where the companion used to vanish. A break
    // is the calmest the app ever is, so it is the last place it should feel
    // empty. There is deliberately no countdown here: the copy asks you to put
    // the phone down, and a ticking clock would argue with that.
    Surface(Modifier.fillMaxSize(), color = Paper) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.7f))

            SquishyStage(
                state = SquishyState.RELAXING,
                progress = 0f,
                accent = Sage,
                material = material,
                reducedMotion = reducedMotion,
                onSquish = {},
                stageSize = 230.dp,
            )

            Spacer(Modifier.height(18.dp))

            Text("BREAK", color = Sage, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(
                block.title,
                color = PaperInk,
                fontSize = 31.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "${block.minutes} minutes · screen-free if you can",
                color = PaperMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PaperInk, contentColor = Sage),
            ) {
                Text(
                    // Block titles are user text and run long, so the label has to
                    // be able to give up rather than push its own button apart.
                    text = if (nextBlock == null) "Complete mission" else "Continue · ${nextBlock.title}",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onFinish) { Text("Finish for today", color = PaperMuted) }
        }
    }
}

@Composable
fun SessionReflectionScreen(
    completedBlock: MissionBlock,
    /**
     * Minutes actually banked, which is not always the block's planned length:
     * the duration picker can override a block before it starts, and reporting
     * the plan instead of the run would credit focus that never happened.
     */
    bankedMinutes: Int,
    material: SquishyMaterial,
    focusedMinutes: Int,
    isPremium: Boolean,
    reducedMotion: Boolean,
    onRated: (SessionFeeling) -> Unit,
    onFinish: () -> Unit,
) {
    // Finishing a block is the emotional peak of the product, and it used to be
    // marked with an empty green rectangle. What somebody has actually just
    // earned is a settled companion and a step along the shelf, so show both.
    val nextToEarn = if (isPremium) null else nextMaterialToEarn(focusedMinutes)

    Surface(Modifier.fillMaxSize(), color = Paper) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(.45f))

            SquishyStage(
                state = SquishyState.RELAXING,
                progress = 1f,
                accent = Sage,
                material = material,
                reducedMotion = reducedMotion,
                onSquish = {},
                stageSize = 236.dp,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "BLOCK COMPLETE",
                color = Sage,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                completedBlock.title,
                color = PaperInk,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            EarnedBanner(minutes = bankedMinutes, next = nextToEarn, focusedMinutes = focusedMinutes)

            Spacer(Modifier.weight(.5f))

            Text("How did that feel?", color = PaperMuted, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf(
                    SessionFeeling.EASY to "Easy",
                    SessionFeeling.RIGHT to "Right",
                    SessionFeeling.TOO_MUCH to "Too much",
                ).forEach { (feeling, label) ->
                    OutlinedButton(
                        onClick = { onRated(feeling) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text(label, fontSize = 12.sp, color = PaperInk) }
                }
            }

            Spacer(Modifier.weight(.35f))
            TextButton(onClick = onFinish) { Text("Finish for today", color = PaperMuted) }
        }
    }
}

/** The minutes just banked, and what they moved you closer to. */
@Composable
private fun EarnedBanner(minutes: Int, next: SquishyMaterial?, focusedMinutes: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "+$minutes min of focus banked",
            color = PaperInk.copy(alpha = 0.62f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        if (next == null) return@Column

        val remaining = (next.unlockMinutes - focusedMinutes).coerceAtLeast(0)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth(0.62f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PaperInk.copy(alpha = 0.10f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((focusedMinutes.toFloat() / next.unlockMinutes).coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Sage),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (remaining == 0) {
                "${next.displayName} unlocked"
            } else {
                "${next.displayName} · ${formatRemaining(remaining)}"
            },
            color = PaperMuted,
            fontSize = 12.sp,
        )
    }
}
