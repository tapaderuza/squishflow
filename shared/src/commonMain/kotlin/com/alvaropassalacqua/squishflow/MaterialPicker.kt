package com.alvaropassalacqua.squishflow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The shelf of bodies.
 *
 * A locked chip shows an arc of how close its focus requirement is rather than a
 * padlock, because the point of [MaterialAccess] is that the person is already on
 * their way to it. Nothing here says "buy" — the price of every body is time, and
 * Pro is only the shortcut.
 */
@Composable
internal fun MaterialPicker(
    selected: SquishyMaterial,
    focusedMinutes: Int,
    isPremium: Boolean,
    onSelect: (SquishyMaterial) -> Unit,
    onLockedTapped: (SquishyMaterial) -> Unit,
    modifier: Modifier = Modifier,
    isTrialling: Boolean = false,
) {
    val selectedAccess = selected.accessWith(focusedMinutes, isPremium)
    val nextToEarn = if (isPremium) null else nextMaterialToEarn(focusedMinutes)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            items(SquishyMaterial.entries, key = { it.name }) { material ->
                val access = material.accessWith(focusedMinutes, isPremium)
                MaterialChip(
                    material = material,
                    access = access,
                    isSelected = material == selected,
                    onClick = {
                        if (access.isUsable) onSelect(material) else onLockedTapped(material)
                    },
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        Text(
            text = when {
                isTrialling -> "Trying ${selected.displayName} — squeeze it while you can."
                selectedAccess is MaterialAccess.Locked ->
                    "${selected.displayName} · ${formatRemaining(selectedAccess.remainingMinutes)}"
                nextToEarn != null ->
                    "Next: ${nextToEarn.displayName} · ${formatRemaining(nextToEarn.unlockMinutes - focusedMinutes)}"
                else -> selected.description
            },
            color = if (isTrialling) Coral else Muted.copy(alpha = 0.8f),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun MaterialChip(
    material: SquishyMaterial,
    access: MaterialAccess,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val locked = access as? MaterialAccess.Locked
    val background by animateColorAsState(
        targetValue = if (isSelected) Ink.copy(alpha = 0.13f) else Ink.copy(alpha = 0.05f),
        label = "chip-background",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (locked != null) 0.5f else 1f,
        label = "chip-alpha",
    )
    val progress by animateFloatAsState(
        targetValue = locked?.progress ?: 1f,
        label = "chip-progress",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, Sage.copy(alpha = 0.55f), RoundedCornerShape(percent = 50))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp)
            .semantics {
                role = Role.Tab
                selected = isSelected
                contentDescription = when (access) {
                    is MaterialAccess.Locked ->
                        "${material.displayName}, ${formatRemaining(access.remainingMinutes)}. ${material.description}"
                    is MaterialAccess.Earned -> "${material.displayName}, earned. ${material.description}"
                    else -> "${material.displayName}. ${material.description}"
                }
            },
    ) {
        MaterialDot(material = material, progress = progress, showRing = locked != null, alpha = contentAlpha)
        Text(
            text = material.displayName,
            color = Ink.copy(alpha = contentAlpha),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/**
 * A dot whose brightness previews the finish, wrapped by an arc of earn progress
 * while the body is still locked.
 */
@Composable
private fun MaterialDot(
    material: SquishyMaterial,
    progress: Float,
    showRing: Boolean,
    alpha: Float,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(if (showRing) 16.dp else 11.dp)) {
        if (showRing) {
            Canvas(Modifier.size(16.dp)) {
                val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(color = Ink.copy(alpha = 0.18f), style = stroke)
                if (progress > 0f) {
                    drawArc(
                        color = Sage.copy(alpha = 0.85f),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = stroke,
                    )
                }
            }
        }
        Box(
            Modifier
                .size(if (showRing) 8.dp else 11.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = (0.18f + material.finish.gloss * 0.9f) * alpha)),
        )
    }
}
