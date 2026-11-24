package com.example.tvclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    fun schedule(context: Context, alarm: AlarmItem): Boolean {
        if (!alarm.isEnabled) return false
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, alarm)
        val triggerAtMillis = AlarmTimeCalculator.nextTriggerAt(
            System.currentTimeMillis(),
            alarm.hour,
            alarm.minute
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                false
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                true
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            false
        }
    }

    fun cancel(context: Context, alarm: AlarmItem) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, alarm))
    }

    fun rescheduleAll(context: Context) {
        PrefsManager(context).getAlarms().filter { it.isEnabled }.forEach {
            schedule(context, it)
        }
    }

    private fun pendingIntent(context: Context, alarm: AlarmItem): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_AUDIO_URI", alarm.audioUri)
            putExtra("ALARM_SPOKEN_TEXT", alarm.spokenText)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
