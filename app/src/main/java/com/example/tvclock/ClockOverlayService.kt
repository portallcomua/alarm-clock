package com.example.tvclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
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
    private var viewAttached = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            PrefsManager(this).isClockActive = false
            stopSelf()
            return
        }

        startForegroundNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val prefs = PrefsManager(this)
        container = FrameLayout(this)

        val isLargeScreen = resources.configuration.screenWidthDp >= 600
        val clockView: View = if (prefs.isAnalog) {
            @Suppress("DEPRECATION")
            AnalogClock(this).apply {
                val size = dp(if (prefs.isFullScreen) { if (isLargeScreen) 520 else 260 } else { if (isLargeScreen) 180 else 120 })
                layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            }
        } else {
            TextClock(this).apply {
                format24Hour = "HH:mm:ss"
                format12Hour = "hh:mm:ss a"
                textSize = if (prefs.isFullScreen) { if (isLargeScreen) 96f else 48f } else 27f
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
            container.setBackgroundColor(Color.BLACK)
        } else {
            container.setBackgroundColor(Color.parseColor("#99000000"))
            container.setPadding(dp(18), dp(10), dp(18), dp(10))
        }
        container.addView(clockView)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val size = if (prefs.isFullScreen) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            WindowManager.LayoutParams.WRAP_CONTENT
        }
        val params = WindowManager.LayoutParams(
            size,
            size,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (!prefs.isFullScreen) {
                gravity = Gravity.TOP or Gravity.END
                x = dp(24)
                y = dp(24)
            } else {
                gravity = Gravity.CENTER
            }
        }

        try {
            windowManager.addView(container, params)
            viewAttached = true
        } catch (_: Exception) {
            prefs.isClockActive = false
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startForegroundNotification() {
        val channelId = "tv_clock_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ТВ Годинник",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Сповіщення про роботу екранного годинника"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ТВ Годинник працює")
            .setContentText("Годинник відображається поверх екрана")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        if (viewAttached && ::container.isInitialized && ::windowManager.isInitialized) {
            try { windowManager.removeView(container) } catch (_: Exception) { }
            viewAttached = false
        }
        super.onDestroy()
    }
}
