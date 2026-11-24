package com.example.tvclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Будильник"
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val audioUri = intent.getStringExtra("ALARM_AUDIO_URI")
        val spokenText = intent.getStringExtra("ALARM_SPOKEN_TEXT") ?: ""

        val ringIntent = Intent(context, AlarmRingService::class.java).apply {
            putExtra("ALARM_LABEL", label)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_AUDIO_URI", audioUri)
            putExtra("ALARM_SPOKEN_TEXT", spokenText)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(ringIntent)
            } else {
                context.startService(ringIntent)
            }
        } catch (_: Exception) {
            // Keep the notification/activity fallback if a device blocks foreground service startup.
            val activityIntent = Intent(context, AlarmRingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("ALARM_LABEL", label)
                putExtra("ALARM_ID", alarmId)
            }
            try { context.startActivity(activityIntent) } catch (_: Exception) { }
        }

        // Saved alarms repeat daily; schedule the next occurrence immediately.
        PrefsManager(context).getAlarms()
            .firstOrNull { it.id == alarmId && it.isEnabled }
            ?.let { AlarmScheduler.schedule(context, it) }
    }
}
