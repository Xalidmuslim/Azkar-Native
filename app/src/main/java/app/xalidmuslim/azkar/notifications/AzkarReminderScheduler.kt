package app.xalidmuslim.azkar.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.xalidmuslim.azkar.azkarPreferencesDataStore
import app.xalidmuslim.azkar.content.AzkarPeriod
import app.xalidmuslim.azkar.persistence.AzkarPreferenceKeys
import app.xalidmuslim.azkar.ui.reading.AzkarReaderSettings
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

internal object AzkarReminderScheduler {
    const val EXTRA_PERIOD = "azkar_reminder_period"

    private const val REQUEST_MORNING = 7101
    private const val REQUEST_EVENING = 7102

    fun sync(context: Context, settings: AzkarReaderSettings) {
        schedule(
            context,
            AzkarPeriod.Morning,
            settings.morningReminderEnabled,
            settings.morningReminderMinutes,
        )
        schedule(
            context,
            AzkarPeriod.Evening,
            settings.eveningReminderEnabled,
            settings.eveningReminderMinutes,
        )
    }

    suspend fun syncFromPreferences(context: Context) {
        val preferences = context.azkarPreferencesDataStore.data.first()
        schedule(
            context,
            AzkarPeriod.Morning,
            preferences[AzkarPreferenceKeys.MorningReminderEnabled] ?: false,
            (preferences[AzkarPreferenceKeys.MorningReminderMinutes] ?: 7 * 60).coerceIn(0, 1439),
        )
        schedule(
            context,
            AzkarPeriod.Evening,
            preferences[AzkarPreferenceKeys.EveningReminderEnabled] ?: false,
            (preferences[AzkarPreferenceKeys.EveningReminderMinutes] ?: 18 * 60).coerceIn(0, 1439),
        )
    }

    suspend fun reschedulePeriodFromPreferences(context: Context, period: AzkarPeriod) {
        val preferences = context.azkarPreferencesDataStore.data.first()
        when (period) {
            AzkarPeriod.Morning -> schedule(
                context,
                period,
                preferences[AzkarPreferenceKeys.MorningReminderEnabled] ?: false,
                (preferences[AzkarPreferenceKeys.MorningReminderMinutes] ?: 7 * 60).coerceIn(0, 1439),
            )
            AzkarPeriod.Evening -> schedule(
                context,
                period,
                preferences[AzkarPreferenceKeys.EveningReminderEnabled] ?: false,
                (preferences[AzkarPreferenceKeys.EveningReminderMinutes] ?: 18 * 60).coerceIn(0, 1439),
            )
        }
    }

    private fun schedule(
        context: Context,
        period: AzkarPeriod,
        enabled: Boolean,
        minuteOfDay: Int,
    ) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent(appContext, period)

        alarmManager.cancel(pendingIntent)
        if (!enabled) return

        val safeMinutes = minuteOfDay.coerceIn(0, 1439)
        val hour = safeMinutes / 60
        val minute = safeMinutes % 60
        val now = ZonedDateTime.now()
        var trigger = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toInstant().toEpochMilli(),
            pendingIntent,
        )
    }

    private fun pendingIntent(context: Context, period: AzkarPeriod): PendingIntent {
        val intent = Intent(context, AzkarReminderReceiver::class.java)
            .putExtra(EXTRA_PERIOD, period.name)
        return PendingIntent.getBroadcast(
            context,
            if (period == AzkarPeriod.Morning) REQUEST_MORNING else REQUEST_EVENING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
