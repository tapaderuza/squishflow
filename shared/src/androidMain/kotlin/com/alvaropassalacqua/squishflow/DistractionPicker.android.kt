package com.alvaropassalacqua.squishflow

import android.content.Intent
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.drawable.Drawable
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Bitmap
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

actual val supportsAppProtection: Boolean = true

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

private data class DistractionApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?,
)

/**
 * The launcher icon, as something Compose can draw.
 *
 * Adaptive icons report no intrinsic size, so they have to be given a canvas
 * rather than asked how big they are. Anything that fails to load falls back to
 * the initial, which is what the whole list used to look like.
 */
private fun Drawable.toIconBitmap(sizePx: Int): ImageBitmap? = runCatching {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    bitmap.asImageBitmap()
}.getOrNull()

@Composable
actual fun DistractionPicker(
    allowance: Int?,
    draft: List<String>?,
    onDraftChanged: (List<String>) -> Unit,
    onUpgrade: () -> Unit,
    onContinue: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    FocusPreferences.initialize(context)
    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager.queryIntentActivities(intent, 0)
            .map {
                DistractionApp(
                    label = it.loadLabel(context.packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = runCatching { it.loadIcon(context.packageManager) }
                        .getOrNull()
                        ?.toIconBitmap(ICON_PX),
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
    var selected by remember { mutableStateOf(draft?.toSet() ?: FocusPreferences.selectedPackages()) }
    LaunchedEffect(selected) { onDraftChanged(selected.toList()) }
    var showDisclosure by remember { mutableStateOf(false) }
    // Set the first time a tap is refused for the limit, and left on: the line
    // that explains it should not blink in and out with every attempt.
    var hitLimit by remember { mutableStateOf(false) }
    val ink = Ink
    val muted = Muted
    val surface = SoftWhite
    val accent = Sage

    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("What steals your focus?", color = ink, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(10.dp))
            Text(
                "Pick the apps you open on impulse. Squishy turns opening them into a decision.",
                color = muted, fontSize = 14.sp, lineHeight = 21.sp,
            )
            if (allowance != null && (hitLimit || selected.size >= allowance)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Free protects $allowance apps. Pro protects every one  →",
                    color = Premium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onUpgrade)
                        .padding(vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    val active = app.packageName in selected
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (active) accent.copy(alpha = 0.12f) else surface, RoundedCornerShape(18.dp))
                            .clickable {
                                selected = when {
                                    active -> selected - app.packageName
                                    allowance == null || selected.size < allowance -> selected + app.packageName
                                    else -> { hitLimit = true; selected }
                                }
                            }
                            .padding(horizontal = 17.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(
                                    if (app.icon != null) Color.Transparent
                                    else if (active) accent else ink.copy(alpha = 0.08f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                )
                            } else {
                                Text(
                                    app.label.take(1).uppercase(),
                                    color = if (active) OnSage else ink,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, color = ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(20.dp).background(if (active) accent else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) Text("✓", color = OnSage, fontSize = 12.sp, fontWeight = FontWeight.Black)
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
                    contentColor = OnLight,
                    disabledContainerColor = surface,
                    disabledContentColor = muted,
                ),
            ) {
                Text(
                    if (selected.isEmpty()) "Pick at least one" else "Turn on protection  ·  ${selected.size}",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            containerColor = SoftWhite,
            titleContentColor = Ink,
            textContentColor = Ink.copy(alpha = 0.72f),
            title = { Text("Focus protection") },
            text = {
                Text(
                    "Squishflow uses Accessibility only to detect when you open an app you picked, so it can show you a pause with Squishy. It never reads, stores or shares text, email or anything on your screen. You can turn it off at any time in Settings.",
                    lineHeight = 20.sp,
                )
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) {
                    Text("Not now", color = Muted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisclosure = false
                        onContinue(selected.toList())

                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Sage,
                        contentColor = OnSage,
                    ),
                ) { Text("I understand — continue", fontWeight = FontWeight.Bold) }
            },
        )
    }}
/** Rendered once per app at a size that survives the densest screens. */
private const val ICON_PX = 144
