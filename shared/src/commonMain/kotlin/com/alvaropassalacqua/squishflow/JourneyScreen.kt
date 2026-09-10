package com.alvaropassalacqua.squishflow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The collection.
 *
 * This screen used to report the timer's in-memory counters, so a lifetime of
 * focus read as zero on every cold start, and it ended in an "unlock patterns"
 * button that led nowhere. Both are gone: the numbers come from storage, and the
 * journey being shown is the real one — which bodies the focus has earned, and
 * how far the next is.
 */
@Composable
fun JourneyScreen(
    stats: LifetimeStats,
    selected: SquishyMaterial,
    isPremium: Boolean,
    onBack: () -> Unit,
    onPremium: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back", color = Muted) }
                Spacer(Modifier.weight(1f))
                Text(
                    "YOUR JOURNEY",
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }

            Spacer(Modifier.height(26.dp))

            FocusTotal(minutes = stats.focusedMinutes)

            Spacer(Modifier.height(26.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JourneyMetric("${stats.completedBlocks}", "BLOCKS\nFINISHED", Modifier.weight(1f))
                JourneyMetric(
                    stats.consistency?.let { "$it%" } ?: "—",
                    "FINISHED\nWHAT I START",
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(30.dp))

            Text(
                "THE SHELF",
                color = Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            SquishyMaterial.entries.forEach { material ->
                CollectionRow(
                    material = material,
                    access = material.accessWith(stats.focusedMinutes, isPremium),
                    isSelected = material == selected,
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "Do not chase a perfect streak.\nJust come back each time you drift.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )

            if (!isPremium) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Or have them all now",
                    color = Ink.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onPremium)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The headline number, phrased as the currency the shelf is bought with. */
@Composable
private fun FocusTotal(minutes: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when {
                minutes < 60 -> "${minutes}m"
                minutes % 60 == 0 -> "${minutes / 60}h"
                else -> "${minutes / 60}h ${minutes % 60}m"
            },
            color = Ink,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "OF FOCUS SO FAR",
            color = Sage,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
        )
    }
}

@Composable
private fun CollectionRow(
    material: SquishyMaterial,
    access: MaterialAccess,
    isSelected: Boolean,
) {
    val locked = access as? MaterialAccess.Locked
    val alpha = if (locked != null) 0.5f else 1f
    val status = when (access) {
        is MaterialAccess.Free -> "Free"
        is MaterialAccess.Earned -> "Earned"
        is MaterialAccess.Purchased -> "With Pro"
        is MaterialAccess.Locked -> shortRemaining(access.remainingMinutes)
    }

    Surface(
        color = if (isSelected) Ink.copy(alpha = 0.09f) else SoftWhite,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = when (access) {
                    is MaterialAccess.Locked ->
                        "${material.displayName}, ${formatRemaining(access.remainingMinutes)}. ${material.description}"
                    else -> "${material.displayName}, $status. ${material.description}"
                }
            },
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp)) {
                if (locked != null) {
                    Canvas(Modifier.size(30.dp)) {
                        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        drawCircle(color = Ink.copy(alpha = 0.16f), style = stroke)
                        if (locked.progress > 0f) {
                            drawArc(
                                color = Sage,
                                startAngle = -90f,
                                sweepAngle = 360f * locked.progress,
                                useCenter = false,
                                style = stroke,
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .size(if (locked != null) 15.dp else 22.dp)
                        .clip(CircleShape)
                        .background(
                            Color.White.copy(alpha = (0.18f + material.finish.gloss * 0.9f) * alpha),
                        ),
                )
            }

            Spacer(Modifier.width(15.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    material.displayName,
                    color = Ink.copy(alpha = alpha),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    material.description,
                    color = Muted.copy(alpha = alpha),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = status,
                color = if (locked != null) Muted else Sage,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(74.dp),
            )
        }
    }
}

@Composable
private fun JourneyMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = SoftWhite, shape = RoundedCornerShape(22.dp)) {
        Column(
            Modifier.padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(5.dp))
            Text(
                label,
                color = Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
        }
    }
}
