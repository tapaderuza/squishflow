package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable

/**
 * The one notification the app sends: the block you started has ended.
 *
 * The whole proposition is that the phone goes face down, so the app is
 * usually not on screen when a block finishes. Without this the finish is
 * silent and the person either keeps working past it or keeps checking, and
 * both are the thing a timer exists to remove. Scheduled at the deadline by
 * the platform's alarm machinery so it fires even if the process is gone.
 *
 * It says what the app would say: no praise, no streak, the minutes banked.
 */
expect object BlockAlarm {
    fun schedule(deadlineEpochMillis: Long, totalSeconds: Int)
    fun cancel()
}

/**
 * Ask for notification permission where the platform needs asking, once
 * [trigger] first becomes positive — in practice, when the first block starts,
 * which is the moment the permission has a reason.
 */
@Composable
expect fun RequestNotificationPermission(trigger: Int)

/** The words on the notification. Kept here so the copy is tested once. */
internal fun blockDoneNotification(totalSeconds: Int): Pair<String, String> {
    val minutes = (totalSeconds / 60).coerceAtLeast(1)
    return "Block complete" to "Banked. $minutes min of focus. Come back when you are ready."
}
