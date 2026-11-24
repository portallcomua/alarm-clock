package com.example.tvclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_TIME_CHANGED &&
            intent.action != Intent.ACTION_TIMEZONE_CHANGED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        AlarmScheduler.rescheduleAll(context)

        val shouldStartClock = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!shouldStartClock) return

        val prefs = PrefsManager(context)
        if (!prefs.autoStartOnBoot) return

        if (!Settings.canDrawOverlays(context)) {
            prefs.isClockActive = false
            return
        }

        val serviceIntent = Intent(context, ClockOverlayService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            prefs.isClockActive = true
        } catch (_: Exception) {
            prefs.isClockActive = false
        }
    }
}
