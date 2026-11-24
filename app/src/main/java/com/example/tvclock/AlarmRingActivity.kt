package com.example.tvclock

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AlarmRingActivity : AppCompatActivity() {
    private var alarmId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        alarmId = intent.getIntExtra("ALARM_ID", 0)
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Будильник"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.parseColor("#991B1B"))
        }
        val title = TextView(this).apply {
            text = "⏰ $label"
            textSize = 42f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        val btnStop = Button(this).apply {
            text = "ВИМКНУТИ БУДИЛЬНИК (OK)"
            textSize = 24f
            setPadding(48, 24, 48, 24)
            isFocusable = true
            setOnClickListener { stopAlarm() }
        }
        layout.addView(title)
        layout.addView(btnStop)
        setContentView(layout)
        btnStop.requestFocus()
    }

    private fun stopAlarm() {
        stopService(Intent(this, AlarmRingService::class.java))
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(alarmId)
        finish()
    }
}
