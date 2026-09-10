package com.example.squishfocus

import android.content.Intent
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

actual object FocusPreferences {
    private const val FILE = "squish_focus"
    private const val ONBOARDED = "onboarded"
    private const val SELECTED = "selected_apps"
    private var appContext: Context? = null

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun initialize(context: Context) { appContext = context.applicationContext }
    fun contextOrNull(): Context? = appContext
    fun selectedPackages(): Set<String> = appContext?.let {
        preferences(it).getStringSet(SELECTED, emptySet()).orEmpty()
    } ?: emptySet()

    actual fun hasCompletedOnboarding(): Boolean =
        appContext?.let { preferences(it).getBoolean(ONBOARDED, false) } ?: false

    actual fun selectedAppCount(): Int = appContext?.let {
        preferences(it).getStringSet(SELECTED, emptySet()).orEmpty().size
    } ?: 0

    actual fun resetOnboarding() {
        appContext?.let { preferences(it).edit().putBoolean(ONBOARDED, false).apply() }
    }

    actual fun isProtectionEnabled(): Boolean {
        val context = appContext ?: return false
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().contains("FocusGuardService")
    }

    actual fun openProtectionSettings() {
        appContext?.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    actual fun completeOnboarding(selectedPackages: List<String>) {
        appContext?.let {
            preferences(it).edit()
                .putBoolean(ONBOARDED, true)
                .putStringSet(SELECTED, selectedPackages.toSet())
                .apply()
        }
    }
}

private data class DistractionApp(val label: String, val packageName: String)

@Composable
actual fun DistractionPicker(onContinue: (List<String>) -> Unit) {
    val context = LocalContext.current
    FocusPreferences.initialize(context)
    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager.queryIntentActivities(intent, 0)
            .map {
                DistractionApp(
                    label = it.loadLabel(context.packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
    var selected by remember { mutableStateOf(FocusPreferences.selectedPackages()) }
    var showDisclosure by remember { mutableStateOf(false) }
    val ink = Color(0xFFF2F0E9)
    val muted = Color(0xFF969991)
    val surface = Color(0xFF171A18)
    val accent = Color(0xFF8DD6AA)

    Surface(Modifier.fillMaxSize(), color = Color(0xFF0C0E0D)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("01 / 01", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(22.dp))
            Text("¿Qué te roba el foco?", color = ink, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(10.dp))
            Text(
                "Elige las apps que sueles abrir por impulso. Squishy las convertirá en una decisión consciente.",
                color = muted, fontSize = 14.sp, lineHeight = 21.sp,
            )
            Spacer(Modifier.height(24.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    val active = app.packageName in selected
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (active) accent.copy(alpha = 0.12f) else surface, RoundedCornerShape(18.dp))
                            .clickable {
                                selected = if (active) selected - app.packageName else selected + app.packageName
                            }
                            .padding(horizontal = 17.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(34.dp).background(if (active) accent else ink.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(app.label.take(1).uppercase(), color = if (active) Color(0xFF102016) else ink, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, color = ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(20.dp).background(if (active) accent else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) Text("✓", color = Color(0xFF102016), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showDisclosure = true },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ink,
                    contentColor = Color(0xFF151713),
                    disabledContainerColor = surface,
                    disabledContentColor = muted,
                ),
            ) {
                Text(
                    if (selected.isEmpty()) "Elige al menos una" else "Activar protección  ·  ${selected.size}",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            containerColor = Color(0xFF171A18),
            titleContentColor = Color(0xFFF2F0E9),
            textContentColor = Color(0xFFB8BBB3),
            title = { Text("Protección de foco") },
            text = {
                Text(
                    "SquishFocus usa Accesibilidad para detectar únicamente cuándo abres una app elegida y mostrarte una pausa con Squishy. No lee, guarda ni comparte textos, correos ni contenido de pantalla. Puedes desactivarlo cuando quieras en Ajustes.",
                    lineHeight = 20.sp,
                )
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) {
                    Text("Ahora no", color = Color(0xFF969991))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisclosure = false
                        onContinue(selected.toList())

                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8DD6AA),
                        contentColor = Color(0xFF102016),
                    ),
                ) { Text("Entiendo y continuar", fontWeight = FontWeight.Bold) }
            },
        )
    }}