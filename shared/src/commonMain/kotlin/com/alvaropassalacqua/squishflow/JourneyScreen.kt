package com.alvaropassalacqua.squishflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val JourneyCream = Color(0xFF0C0E0D)
private val JourneyInk = Color(0xFFF2F0E9)
private val JourneyMuted = Color(0xFF969991)
private val JourneySage = Color(0xFF8DD6AA)
private val JourneyCoral = Color(0xFFFF806C)
private val JourneyWhite = Color(0xFF171A18)

@Composable
fun JourneyScreen(state: TimerUiState, onBack: () -> Unit, onPremium: () -> Unit) {
    val attempts = state.completedSessions + state.failedSessions
    val consistency = if (attempts == 0) 0 else state.completedSessions * 100 / attempts
    Surface(Modifier.fillMaxSize(), color = JourneyCream) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back", color = JourneyMuted) }
                Spacer(Modifier.weight(1f))
                Text("YOUR JOURNEY", color = JourneyInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.size(112.dp).clip(CircleShape).background(JourneySage.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEVEL", color = JourneySage, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(state.level.toString(), color = JourneyInk, fontSize = 44.sp, fontWeight = FontWeight.Light)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                when (state.level) {
                    1 -> "Squishy wakes up"
                    2 -> "Squishy finds its calm"
                    else -> "Squishy protects your attention"
                },
                color = JourneyInk, fontSize = 24.sp, fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${3 - (state.completedSessions % 3)} sessions to the next evolution",
                color = JourneyMuted, fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { state.levelProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = JourneySage,
                trackColor = JourneyInk.copy(alpha = 0.08f),
            )
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JourneyMetric("${state.focusedSeconds / 60}", "MINUTES\nIN FOCUS", Modifier.weight(1f))
                JourneyMetric("${state.completedSessions}", "SESSIONS\nCOMPLETED", Modifier.weight(1f))
                JourneyMetric("$consistency%", "CONSISTENCY", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Surface(color = JourneyWhite, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(JourneyCoral.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                        Text("MOVE", color = JourneyCoral, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Time recovered", color = JourneyInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("You turned movement into ${state.rescuedSeconds / 60} minutes of focus.", color = JourneyMuted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Do not chase a perfect streak.\nJust come back each time you drift.",
                color = JourneyMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = onPremium,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JourneyInk.copy(alpha = 0.15f)),
            ) {
                Text("Unlock patterns with Premium", color = JourneyInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun JourneyMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = JourneyWhite, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(vertical = 18.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = JourneyInk, fontSize = 25.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(5.dp))
            Text(label, color = JourneyMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}