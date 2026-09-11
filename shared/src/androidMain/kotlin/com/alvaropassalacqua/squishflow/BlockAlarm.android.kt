package com.alvaropassalacqua.squishflow

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

private const val CHANNEL = "blocks"
private const val REQUEST = 4_101
private const val EXTRA_TOTAL = "total_seconds"

actual object BlockAlarm {
    actual fun schedule(deadlineEpochMillis: Long, totalSeconds: Int) {
        val context = FocusPreferences.contextOrNull() ?: return
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val fire = PendingIntent.getBroadcast(
            context, REQUEST,
            Intent(context, BlockAlarmReceiver::class.java).putExtra(EXTRA_TOTAL, totalSeconds),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // setAlarmClock is exact and survives Doze. It needs USE_EXACT_ALARM,
        // which Play reserves for alarm and timer apps — this is a timer, and
        // the manifest declares it. Should a device still refuse, the block
        // ends with an inexact alarm rather than in silence.
        val show = PendingIntent.getActivity(
            context, REQUEST, launchIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(deadlineEpochMillis, show), fire)
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineEpochMillis, fire)
        }
    }

    actual fun cancel() {
        val context = FocusPreferences.contextOrNull() ?: return
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val fire = PendingIntent.getBroadcast(
            context, REQUEST, Intent(context, BlockAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.cancel(fire)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(REQUEST)
    }
}

private fun launchIntent(context: Context): Intent =
    context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)

/** Fires at the deadline and posts the one notification. Declared in the manifest. */
class BlockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Block complete", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "One notification when a focus block you started has ended."
                },
            )
        }
        val (title, text) = blockDoneNotification(intent.getIntExtra(EXTRA_TOTAL, 0))
        val open = PendingIntent.getActivity(
            context, REQUEST, launchIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(com.alvaropassalacqua.squishflow.shared.R.drawable.ic_block_done)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(REQUEST, notification)
    }
}

@Composable
actual fun RequestNotificationPermission(trigger: Int) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(trigger) {
        if (trigger <= 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
