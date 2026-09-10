package com.alvaropassalacqua.squishflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SQUEEZES_TO_CONTINUE = 3

/**
 * The first thing anyone sees.
 *
 * Before this existed the app opened on a list of installed apps and an
 * Accessibility permission request, which asks a stranger for an intrusive
 * permission before showing them anything worth having. Someone evaluating the
 * app in thirty seconds would never reach the companion at all.
 *
 * Now the first screen is the product: one body, no chrome, and nothing to read
 * before you touch it. Protection setup moves behind the header, where it is
 * offered once the app has earned the right to ask.
 */
@Composable
internal fun WelcomeScreen(onDone: () -> Unit) {
    var squeezes by remember { mutableIntStateOf(0) }
    val ready = squeezes >= SQUEEZES_TO_CONTINUE
    val promptAlpha by animateFloatAsState(
        targetValue = if (ready) 0f else 1f,
        label = "welcome-prompt",
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Text(
                "SQUISHFLOW",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.weight(0.5f))

            Text(
                text = if (ready) "Good. That is the whole idea." else "Meet Squishy.",
                color = Ink,
                fontSize = 34.sp,
                // Without this the default leading is tighter than the ascenders,
                // so a two-line headline collides with itself.
                lineHeight = 41.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (ready) {
                    "Focus is going to be hard sometimes. This is what you hold when it is."
                } else {
                    "Push your thumb into it. It pushes back."
                },
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.weight(0.4f))

            SquishyStage(
                state = if (ready) SquishyState.RELAXING else SquishyState.TENSE,
                progress = squeezes.toFloat() / SQUEEZES_TO_CONTINUE,
                accent = if (ready) Sage else Coral,
                onSquish = { squeezes = (squeezes + 1).coerceAtMost(SQUEEZES_TO_CONTINUE) },
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(promptAlpha),
            ) {
                repeat(SQUEEZES_TO_CONTINUE) { index ->
                    Box(
                        Modifier
                            .size(if (index < squeezes) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (index < squeezes) Sage else Ink.copy(alpha = 0.16f)),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(visible = ready, enter = fadeIn(), exit = fadeOut()) {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink,
                        contentColor = Cream,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                ) {
                    Text("Now tell it what to protect", fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
