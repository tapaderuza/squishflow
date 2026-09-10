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
import androidx.compose.ui.text.font.FontWeight
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
    val ink = Color(0xFF101310)
    val paper = Color(0xFFF2F0E9)
    val sage = Color(0xFF8DD6AA)

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
    val ink = Color(0xFF101310)
    val paper = Color(0xFFF2F0E9)
    val sage = Color(0xFF8DD6AA)
    Surface(Modifier.fillMaxSize(), color = paper) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(26.dp)) {
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
                    IconButton(onClick = { onDurationChanged(index, (block.minutes - 5).coerceAtLeast(5)) }) { Text("−") }
                    Text("${block.minutes}m", color = if (block.kind == BlockKind.BREAK) ink.copy(alpha = .45f) else sage, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onDurationChanged(index, (block.minutes + 5).coerceAtMost(60)) }) { Text("+") }
                }
            }
            Spacer(Modifier.weight(1f))
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
    onContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    val ink = Color(0xFF101310)
    val paper = Color(0xFFF2F0E9)
    val sage = Color(0xFF8DD6AA)
    Surface(Modifier.fillMaxSize(), color = paper) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text("BREAK", color = sage, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(
                block.title,
                color = ink,
                fontSize = 31.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(12.dp))
            Text("${block.minutes} minutes · screen-free if you can", color = ink.copy(alpha = .5f), fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = sage),
            ) { Text(if (nextBlock == null) "Complete mission" else "Continue · ${nextBlock.title}", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onFinish) { Text("Finish for today", color = ink.copy(alpha = .5f)) }
        }
    }
}

@Composable
fun SessionReflectionScreen(
    completedBlock: MissionBlock,
    onRated: (SessionFeeling) -> Unit,
    onFinish: () -> Unit,
) {
    val ink = Color(0xFF101310)
    val paper = Color(0xFFF2F0E9)
    val sage = Color(0xFF8DD6AA)
    Surface(Modifier.fillMaxSize(), color = paper) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(.7f))
            Box(Modifier.size(104.dp).background(sage.copy(alpha = .18f), RoundedCornerShape(38.dp)))
            Spacer(Modifier.height(28.dp))
            Text("Block complete", color = sage, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
            Text(
                completedBlock.title,
                color = ink,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(12.dp))
            Text("How did that feel?", color = ink.copy(alpha = .55f), fontSize = 15.sp)
            Spacer(Modifier.height(26.dp))
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
                    ) { Text(label, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onFinish) { Text("Finish for today", color = ink.copy(alpha = .5f)) }
        }
    }
}
