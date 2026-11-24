package com.example.tvclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextClock
import androidx.core.app.NotificationCompat

class ClockOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var clockView: TextClock

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Системний віджет, який сам оновлює час щосекунди
        clockView = TextClock(this).apply {
            format24Hour = "HH:mm"
            textSize = 28f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#80000000")) // Напівпрозорий чорний фон
            setPadding(24, 12, 24, 12)
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END // Правий верхній кут
            x = 40
            y = 40
        }

        windowManager.addView(clockView, params)
    }

    private fun startForegroundNotification() {
        val channelId = "clock_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "ТВ Годинник", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Годинник активний")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::clockView.isInitialized) {
            windowManager.removeView(clockView)
        }
    }
}
