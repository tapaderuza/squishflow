package com.alvaropassalacqua.squishflow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
 * The Pro shelf.
 *
 * This is what an entitlement actually buys. Each chip is a different soft-body
 * tuning, so tapping one changes how the companion answers your thumb rather than
 * recolouring it — which is also why a locked chip still previews its name and
 * character instead of hiding behind a padlock and nothing else.
 */
@Composable
internal fun MaterialPicker(
    selected: SquishyMaterial,
    isPremium: Boolean,
    onSelect: (SquishyMaterial) -> Unit,
    onLockedTapped: (SquishyMaterial) -> Unit,
    modifier: Modifier = Modifier,
    isTrialling: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            items(SquishyMaterial.entries, key = { it.name }) { material ->
                val locked = material.isPro && !isPremium
                MaterialChip(
                    material = material,
                    isSelected = material == selected,
                    isLocked = locked,
                    onClick = { if (locked) onLockedTapped(material) else onSelect(material) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isTrialling) {
                "Trying ${selected.displayName} — squeeze it while you can."
            } else {
                selected.description
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
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (isSelected) Ink.copy(alpha = 0.13f) else Ink.copy(alpha = 0.05f),
        label = "chip-background",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isLocked) 0.45f else 1f,
        label = "chip-alpha",
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
                contentDescription = if (isLocked) {
                    "${material.displayName}, included with Pro. ${material.description}"
                } else {
                    "${material.displayName}. ${material.description}"
                }
            },
    ) {
        // A dot whose finish previews the material: glossy ones read brighter.
        Box(
            Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(
                    Color.White.copy(alpha = (0.18f + material.finish.gloss * 0.9f) * contentAlpha),
                ),
        )
        Text(
            text = material.displayName,
            color = Ink.copy(alpha = contentAlpha),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
        if (isLocked) {
            Text("PRO", color = Coral, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        }
    }
}
