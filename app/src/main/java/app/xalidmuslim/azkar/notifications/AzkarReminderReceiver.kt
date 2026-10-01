package app.xalidmuslim.azkar.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.xalidmuslim.azkar.MainActivity
import app.xalidmuslim.azkar.R
import app.xalidmuslim.azkar.content.AzkarPeriod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AzkarReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val period = intent.getStringExtra(AzkarReminderScheduler.EXTRA_PERIOD)
            ?.let { runCatching { AzkarPeriod.valueOf(it) }.getOrNull() }
            ?: return

        showNotification(context, period)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AzkarReminderScheduler.reschedulePeriodFromPreferences(
                    context.applicationContext,
                    period,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, period: AzkarPeriod) {
        createChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AzkarReminderScheduler.EXTRA_PERIOD, period.name)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            if (period == AzkarPeriod.Morning) 7201 else 7202,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (period == AzkarPeriod.Morning) "Утренние азкары" else "Вечерние азкары"
        val text = if (period == AzkarPeriod.Morning) {
            "Напоминание: уделите время утренним азкарам."
        } else {
            "Напоминание: уделите время вечерним азкарам."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(
            if (period == AzkarPeriod.Morning) 7301 else 7302,
            notification,
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Напоминания об азкарах",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Утренние и вечерние напоминания"
            },
        )
    }

    private companion object {
        const val CHANNEL_ID = "azkar_daily_reminders"
    }
}
