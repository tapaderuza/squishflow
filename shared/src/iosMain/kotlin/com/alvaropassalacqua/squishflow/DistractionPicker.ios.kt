package com.alvaropassalacqua.squishflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import platform.Foundation.NSUserDefaults

actual object FocusPreferences {
    actual fun hasCompletedOnboarding(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("squish_onboarded")

    actual fun selectedAppCount(): Int =
        (NSUserDefaults.standardUserDefaults.arrayForKey("squish_selected_categories")?.size ?: 0).toInt()

    actual fun resetOnboarding() {
        NSUserDefaults.standardUserDefaults.setBool(false, "squish_onboarded")
    }

    actual fun isProtectionEnabled(): Boolean = true
    actual fun openProtectionSettings() = Unit

    actual fun completeOnboarding(selectedPackages: List<String>) {
        NSUserDefaults.standardUserDefaults.setBool(true, "squish_onboarded")
        NSUserDefaults.standardUserDefaults.setObject(selectedPackages, "squish_selected_categories")
    }
}

@Composable
actual fun DistractionPicker(onContinue: (List<String>) -> Unit) {
    val choices = listOf("Social", "Video", "Messaging", "News", "Games", "Shopping")
    var selected by remember { mutableStateOf(setOf<String>()) }
    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("What steals your focus?", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(10.dp))
            Text("Pick your usual distractions. Per-app selection arrives with Family Controls.", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            choices.forEach { choice ->
                val active = choice in selected
                Text(
                    choice,
                    color = Ink,
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(if (active) Sage.copy(alpha = 0.15f) else SoftWhite, RoundedCornerShape(18.dp))
                        .clickable { selected = if (active) selected - choice else selected + choice }
                        .padding(18.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onContinue(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
            ) { Text("Meet Squishy", fontWeight = FontWeight.Bold) }
        }
    }
}