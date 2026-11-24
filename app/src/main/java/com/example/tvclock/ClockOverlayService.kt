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
import android.view.View
import android.view.WindowManager
import android.widget.AnalogClock
import android.widget.FrameLayout
import android.widget.TextClock
import androidx.core.app.NotificationCompat

class ClockOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var container: FrameLayout

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        val prefs = PrefsManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        container = FrameLayout(this)

        val clockView: View = if (prefs.isAnalog) {
            @Suppress("DEPRECATION")
            AnalogClock(this).apply {
                val size = if (prefs.isFullScreen) 500 else 180
                layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            }
        } else {
            TextClock(this).apply {
                format24Hour = "HH:mm:ss"
                textSize = if (prefs.isFullScreen) 96f else 28f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            }
        }

        if (prefs.isFullScreen) {
            container.setBackgroundColor(Color.BLACK) // Повний чорний екран як заставка
        } else {
            container.setBackgroundColor(Color.parseColor("#99000000")) // Напівпрозорий фон у кутку
            container.setPadding(24, 12, 24, 12)
        }
        container.addView(clockView)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val width = if (prefs.isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT
        val height = if (prefs.isFullScreen) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT

        val params = WindowManager.LayoutParams(
            width,
            height,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (!prefs.isFullScreen) {
                gravity = Gravity.TOP or Gravity.END
                x = 40
                y = 40
            }
        }

        windowManager.addView(container, params)
    }

    private fun startForegroundNotification() {
        val channelId = "tv_clock_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "ТВ Годинник", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ТВ Годинник працює")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::container.isInitialized) windowManager.removeView(container)
    }
}
