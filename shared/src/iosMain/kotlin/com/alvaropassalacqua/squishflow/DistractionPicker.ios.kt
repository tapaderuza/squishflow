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
    val choices = listOf("Redes sociales", "Video", "Mensajeria", "Noticias", "Juegos", "Compras")
    var selected by remember { mutableStateOf(setOf<String>()) }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF0C0E0D)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("¿Qué te roba el foco?", color = Color(0xFFF2F0E9), fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(10.dp))
            Text("Elige tus distracciones habituales. La selección específica de apps se activará con Family Controls.", color = Color(0xFF969991), fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            choices.forEach { choice ->
                val active = choice in selected
                Text(
                    choice,
                    color = Color(0xFFF2F0E9),
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(if (active) Color(0xFF8DD6AA).copy(alpha = 0.15f) else Color(0xFF171A18), RoundedCornerShape(18.dp))
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
            ) { Text("Conocer a Squishy", fontWeight = FontWeight.Bold) }
        }
    }
}