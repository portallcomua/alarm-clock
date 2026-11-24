package com.example.tvclock

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var alarmsContainer: LinearLayout
    private lateinit var clockButton: Button
    private lateinit var modeButton: Button
    private lateinit var styleButton: Button
    private lateinit var bootButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)
        requestNotificationPermissionIfNeeded()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(15, 23, 42))
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = "⏰ ТВ Годинник та Будильник"
            textSize = 25f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "Годинник для екрана та будильники на кожен день."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, dp(18))
        })

        clockButton = createTvButton("")
        clockButton.setOnClickListener { toggleClock() }
        root.addView(clockButton)

        modeButton = createTvButton("")
        modeButton.setOnClickListener {
            prefs.isFullScreen = !prefs.isFullScreen
            updateButtons()
            restartOverlayIfActive()
        }
        root.addView(modeButton)

        styleButton = createTvButton("")
        styleButton.setOnClickListener {
            prefs.isAnalog = !prefs.isAnalog
            updateButtons()
            restartOverlayIfActive()
        }
        root.addView(styleButton)

        bootButton = createTvButton("")
        bootButton.setOnClickListener {
            prefs.autoStartOnBoot = !prefs.autoStartOnBoot
            updateButtons()
            Toast.makeText(
                this,
                if (prefs.autoStartOnBoot) "Автозапуск увімкнено" else "Автозапуск вимкнено",
                Toast.LENGTH_SHORT
            ).show()
        }
        root.addView(bootButton)
        updateButtons()

        root.addView(TextView(this).apply {
            text = "Мої будильники"
            textSize = 21f
            setTextColor(Color.rgb(56, 189, 248))
            setPadding(0, dp(26), 0, dp(10))
        })

        val addButton = createTvButton("＋  Додати будильник")
        addButton.setOnClickListener { showAlarmDialog() }
        root.addView(addButton)

        alarmsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        root.addView(alarmsContainer)
        scroll.addView(root)
        setContentView(scroll)
        renderAlarmsList()
        clockButton.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) {
            AlarmScheduler.rescheduleAll(this)
            if (::clockButton.isInitialized) updateButtons()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun updateButtons() {
        clockButton.text = if (prefs.isClockActive) {
            "🟢  Годинник: УВІМКНЕНО"
        } else {
            "⚪  Годинник: ВИМКНЕНО"
        }
        modeButton.text = if (prefs.isFullScreen) {
            "▣  Відображення: НА ВЕСЬ ЕКРАН"
        } else {
            "↗  Відображення: У КУТКУ"
        }
        styleButton.text = if (prefs.isAnalog) {
            "◷  Стиль: СТРІЛКОВИЙ"
        } else {
            "◷  Стиль: ЕЛЕКТРОННИЙ"
        }
        bootButton.text = if (prefs.autoStartOnBoot) {
            "🚀  Автозапуск після ввімкнення: ТАК"
        } else {
            "⏻  Автозапуск після ввімкнення: НІ"
        }
    }

    private fun createTvButton(label: String): Button = Button(this).apply {
        text = label
        textSize = 16f
        isAllCaps = false
        setTextColor(Color.WHITE)
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setPadding(dp(18), dp(13), dp(18), dp(13))
        isFocusable = true
        isFocusableInTouchMode = true

        val focused = GradientDrawable().apply {
            setColor(Color.rgb(37, 99, 235))
            setStroke(dp(3), Color.rgb(250, 204, 21))
            cornerRadius = dp(12).toFloat()
        }
        val normal = GradientDrawable().apply {
            setColor(Color.rgb(30, 41, 59))
            setStroke(dp(1), Color.rgb(51, 65, 85))
            cornerRadius = dp(12).toFloat()
        }
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, dp(10)) }
    }

    private fun createSmallButton(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            minHeight = dp(44)
            setPadding(dp(5), dp(4), dp(5), dp(4))
            isFocusable = true
            setOnClickListener { onClick() }
        }

    private fun toggleClock() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Надайте дозвіл «Поверх інших вікон», щоб показувати годинник поверх програм.",
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, "Не вдалося відкрити налаштування дозволів.", Toast.LENGTH_LONG).show()
            }
            return
        }

        if (prefs.isClockActive) {
            stopService(Intent(this, ClockOverlayService::class.java))
            prefs.isClockActive = false
        } else {
            try {
                val serviceIntent = Intent(this, ClockOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
                else startService(serviceIntent)
                prefs.isClockActive = true
            } catch (_: Exception) {
                prefs.isClockActive = false
                Toast.makeText(this, "Не вдалося запустити годинник. Перевірте дозволи системи.", Toast.LENGTH_LONG).show()
            }
        }
        updateButtons()
    }

    private fun restartOverlayIfActive() {
        if (!prefs.isClockActive) return
        val intent = Intent(this, ClockOverlayService::class.java)
        stopService(intent)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        } catch (_: Exception) {
            prefs.isClockActive = false
            updateButtons()
            Toast.makeText(this, "Годинник зупинено: система не дозволила перезапуск.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAlarmDialog(existing: AlarmItem? = null) {
        var hour = existing?.hour ?: 7
        var minute = existing?.minute ?: 30

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
            setBackgroundColor(Color.rgb(30, 41, 59))
        }
        val timeDisplay = TextView(this).apply {
            text = formatTime(hour, minute)
            textSize = 38f
            setTextColor(Color.rgb(250, 204, 21))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(12))
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        fun refreshTime() {
            timeDisplay.text = formatTime(hour, minute)
        }

        controls.addView(createSmallButton("− Год") {
            hour = (hour + 23) % 24
            refreshTime()
        })
        controls.addView(createSmallButton("+ Год") {
            hour = (hour + 1) % 24
            refreshTime()
        })
        controls.addView(createSmallButton("− 5 хв") {
            minute = (minute + 55) % 60
            refreshTime()
        })
        controls.addView(createSmallButton("+ 5 хв") {
            minute = (minute + 5) % 60
            refreshTime()
        })

        val labelInput = EditText(this).apply {
            hint = "Назва будильника"
            setText(existing?.label ?: "")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            textSize = 16f
            isSingleLine = true
            isFocusable = true
        }

        layout.addView(timeDisplay)
        layout.addView(controls)
        layout.addView(labelInput)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Новий будильник" else "Редагувати будильник")
            .setView(layout)
            .setPositiveButton("Зберегти") { _, _ ->
                val alarms = prefs.getAlarms()
                val label = labelInput.text.toString().trim().ifBlank { "Будильник" }
                if (existing == null) {
                    var id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt().coerceAtLeast(1)
                    while (alarms.any { it.id == id }) {
                        id = if (id == Int.MAX_VALUE) 1 else id + 1
                    }
                    val created = AlarmItem(id, hour, minute, label, true)
                    alarms.add(created)
                    prefs.saveAlarms(alarms)
                    scheduleSystemAlarm(created)
                } else {
                    AlarmScheduler.cancel(this, existing)
                    val index = alarms.indexOfFirst { it.id == existing.id }
                    if (index >= 0) {
                        val updated = existing.copy(hour = hour, minute = minute, label = label)
                        alarms[index] = updated
                        prefs.saveAlarms(alarms)
                        scheduleSystemAlarm(updated)
                    }
                }
                renderAlarmsList()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun renderAlarmsList() {
        alarmsContainer.removeAllViews()
        val alarms = prefs.getAlarms().sortedWith(compareBy<AlarmItem> { it.hour }.thenBy { it.minute })
        if (alarms.isEmpty()) {
            alarmsContainer.addView(TextView(this).apply {
                text = "Поки що будильників немає. Додайте перший кнопкою вище."
                textSize = 15f
                setTextColor(Color.LTGRAY)
                setPadding(dp(4), dp(12), dp(4), dp(12))
            })
            return
        }

        alarms.forEach { alarm ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(30, 41, 59))
                    setStroke(dp(1), Color.rgb(51, 65, 85))
                    cornerRadius = dp(10).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(10)) }
            }
            card.addView(TextView(this).apply {
                text = "${formatTime(alarm.hour, alarm.minute)}  ·  ${alarm.label}"
                textSize = 19f
                setTextColor(if (alarm.isEnabled) Color.WHITE else Color.GRAY)
                setPadding(dp(4), dp(4), dp(4), dp(8))
            })
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            fun actionButton(text: String, action: () -> Unit) {
                actions.addView(createSmallButton(text, action), LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { setMargins(dp(2), 0, dp(2), 0) })
            }
            actionButton(if (alarm.isEnabled) "Вимкнути" else "Увімкнути") {
                val current = prefs.getAlarms()
                val item = current.firstOrNull { it.id == alarm.id } ?: return@actionButton
                item.isEnabled = !item.isEnabled
                prefs.saveAlarms(current)
                if (item.isEnabled) scheduleSystemAlarm(item) else AlarmScheduler.cancel(this, item)
                renderAlarmsList()
            }
            actionButton("Змінити") { showAlarmDialog(alarm) }
            actionButton("Видалити") {
                AlarmScheduler.cancel(this, alarm)
                val current = prefs.getAlarms()
                current.removeAll { it.id == alarm.id }
                prefs.saveAlarms(current)
                renderAlarmsList()
                Toast.makeText(this, "Будильник видалено", Toast.LENGTH_SHORT).show()
            }
            card.addView(actions)
            alarmsContainer.addView(card)
        }
    }

    private fun scheduleSystemAlarm(alarm: AlarmItem) {
        if (!alarm.isEnabled) return
        val exact = AlarmScheduler.schedule(this, alarm)
        if (!exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(android.app.AlarmManager::class.java)
            if (!manager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Для точного спрацювання дозвольте точні будильники в налаштуваннях Android.",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (_: Exception) {
                    Toast.makeText(this, "Відкрийте дозвіл вручну в налаштуваннях програм.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Будильник встановлено з резервним системним плануванням.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int) = String.format("%02d:%02d", hour, minute)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
