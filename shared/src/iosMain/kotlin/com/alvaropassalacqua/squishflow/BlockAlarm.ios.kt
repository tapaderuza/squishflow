package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSDate
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

private const val IDENTIFIER = "squishflow.block"

actual object BlockAlarm {
    actual fun schedule(deadlineEpochMillis: Long, totalSeconds: Int) {
        val seconds = deadlineEpochMillis / 1000.0 - NSDate().timeIntervalSince1970
        if (seconds <= 1.0) return
        val (title, body) = blockDoneNotification(totalSeconds)
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(IDENTIFIER, content, trigger)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, withCompletionHandler = null)
    }

    actual fun cancel() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(IDENTIFIER))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(IDENTIFIER))
    }
}

@Composable
actual fun RequestNotificationPermission(trigger: Int) {
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { _, _ -> }
    }
}
