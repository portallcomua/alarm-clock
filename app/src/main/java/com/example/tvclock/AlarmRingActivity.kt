package com.example.tvclock

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlarmRingActivity : AppCompatActivity() {
    private var alarmId: Int = 0
    private var alarmLabel: String = "Будильник"

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
        alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Будильник"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(127, 29, 29))
        }
        val title = TextView(this).apply {
            text = "⏰ $alarmLabel"
            textSize = 40f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 36)
        }
        val stopButton = Button(this).apply {
            text = "ВИМКНУТИ БУДИЛЬНИК"
            textSize = 21f
            isAllCaps = false
            setPadding(36, 20, 36, 20)
            isFocusable = true
            setOnClickListener { stopAlarm() }
        }
        val snoozeButton = Button(this).apply {
            text = "Відкласти на 5 хвилин"
            textSize = 19f
            isAllCaps = false
            setPadding(36, 18, 36, 18)
            isFocusable = true
            setOnClickListener { snoozeAlarm() }
        }
        layout.addView(title)
        layout.addView(stopButton)
        layout.addView(snoozeButton)
        setContentView(layout)
        stopButton.requestFocus()
    }

    private fun snoozeAlarm() {
        val requestCode = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt().coerceAtLeast(1)
        val snoozeId = requestCode
        val receiverIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_LABEL", "$alarmLabel — повтор")
            putExtra("ALARM_ID", snoozeId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() + 5 * 60 * 1000L

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            stopAlarm()
            Toast.makeText(this, "Будильник повториться через 5 хвилин.", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            stopAlarm()
            Toast.makeText(this, "Повторний сигнал заплановано приблизно через 5 хвилин.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAlarm() {
        stopService(Intent(this, AlarmRingService::class.java))
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(2)
        finish()
    }
}
