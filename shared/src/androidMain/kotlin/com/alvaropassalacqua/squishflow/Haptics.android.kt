package com.alvaropassalacqua.squishflow

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { AndroidHaptics(context) }
}

/**
 * Amplitude-controlled haptics.
 *
 * `View.performHapticFeedback` only offers a fixed set of system accents, and the
 * useful ones for a squishy (`CONFIRM`, `GESTURE_START`) need API 30. Driving the
 * vibrator directly gives per-accent intensity on every supported device and
 * degrades cleanly: amplitude first, then plain durations, then silence.
 */
private class AndroidHaptics(context: Context) : Haptics {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()?.takeIf { it.hasVibrator() }

    private val hasAmplitudeControl: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasAmplitudeControl() == true

    /** Continuous deformation would otherwise fire on every frame and buzz. */
    private var lastDeformAt = 0L

    override fun play(accent: HapticAccent) {
        val device = vibrator ?: return
        if (accent == HapticAccent.DEFORM) {
            val now = android.os.SystemClock.uptimeMillis()
            if (now - lastDeformAt < DEFORM_THROTTLE_MS) return
            lastDeformAt = now
        }
        val (durationMs, amplitude) = when (accent) {
            HapticAccent.TOUCH -> 12L to 90
            HapticAccent.DEFORM -> 8L to 55
            HapticAccent.RELEASE -> 18L to 140
            HapticAccent.REWARD -> 32L to 210
            HapticAccent.REFUSAL -> 26L to 120
        }
        runCatching {
            when {
                hasAmplitudeControl ->
                    device.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    device.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE),
                    )

                else -> {
                    @Suppress("DEPRECATION")
                    device.vibrate(durationMs)
                }
            }
        }
    }

    private companion object {
        const val DEFORM_THROTTLE_MS = 55L
    }
}
