package com.example.tvclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private var isClockRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Простий інтерфейс, зручний для пульта ТВ і пальця
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "Меню Годинника"
            textSize = 24f
        }

        val btnToggle = Button(this).apply {
            text = "Увімкнути / Вимкнути годинник"
            isFocusable = true // Важливо для пульта ТВ
            setOnClickListener { toggleClockOverlay() }
        }

        val btnSettings = Button(this).apply {
            text = "Налаштування (Будильник)"
            isFocusable = true
            setOnClickListener { openAlarmPicker() }
        }

        layout.addView(title)
        layout.addView(btnToggle)
        layout.addView(btnSettings)
        setContentView(layout)
        
        btnToggle.requestFocus() // Фокус на першу кнопку при відкритті
    }

    private fun toggleClockOverlay() {
        // Перевірка дозволу на показ поверх інших вікон
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Надайте дозвіл 'Поверх інших вікон'", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        val serviceIntent = Intent(this, ClockOverlayService::class.java)
        if (isClockRunning) {
            stopService(serviceIntent)
            isClockRunning = false
            Toast.makeText(this, "Годинник вимкнено", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            isClockRunning = true
            Toast.makeText(this, "Годинник увімкнено", Toast.LENGTH_SHORT).show()
            finish() // Закриваємо меню, залишаючи годинник на екрані
        }
    }

    private fun openAlarmPicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            setAlarm(hour, minute)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun setAlarm(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Toast.makeText(this, "Будильник встановлено на $hour:${String.format("%02d", minute)}", Toast.LENGTH_LONG).show()
    }
}
