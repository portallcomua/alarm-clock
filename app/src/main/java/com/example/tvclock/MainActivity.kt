package com.example.tvclock

import android.Manifest

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var alarmsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 36)
        }

        val header = TextView(this).apply {
            text = "⏰ ТВ Годинник та Будильник"
            textSize = 26f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 24)
        }
        root.addView(header)

        // 1. Кнопка Увімкнути / Вимкнути годинник
        val btnToggleClock = createTvButton(
            if (prefs.isClockActive) "🟢 Годинник: УВІМКНЕНО (Натисніть, щоб вимкнути)"
            else "⚪ Годинник: ВИМКНЕНО (Натисніть, щоб увімкнути)"
        )
        btnToggleClock.setOnClickListener {
            toggleClock(btnToggleClock)
        }
        root.addView(btnToggleClock)

        // 2. Режим відображення: Кут екрана чи Весь екран
        val btnMode = createTvButton(
            if (prefs.isFullScreen) "🖥️ Режим: НА ВЕСЬ ЕКРАН (як заставка)"
            else "↗️ Режим: В ПРАВОМУ ВЕРХНЬОМУ КУТКУ"
        )
        btnMode.setOnClickListener {
            prefs.isFullScreen = !prefs.isFullScreen
            btnMode.text = if (prefs.isFullScreen) "🖥️ Режим: НА ВЕСЬ ЕКРАН (як заставка)"
            else "↗️ Режим: В ПРАВОМУ ВЕРХНЬОМУ КУТКУ"
            restartOverlayIfActive()
        }
        root.addView(btnMode)

        // 3. Вигляд: Електронний чи Циферблат
        val btnStyle = createTvButton(
            if (prefs.isAnalog) "🕰️ Вигляд: СТРІЛКОВИЙ ЦИФЕРБЛАТ"
            else "🔢 Вигляд: ЕЛЕКТРОННИЙ ГОДИННИК"
        )
        btnStyle.setOnClickListener {
            prefs.isAnalog = !prefs.isAnalog
            btnStyle.text = if (prefs.isAnalog) "🕰️ Вигляд: СТРІЛКОВИЙ ЦИФЕРБЛАТ"
            else "🔢 Вигляд: ЕЛЕКТРОННИЙ ГОДИННИК"
            restartOverlayIfActive()
        }
        root.addView(btnStyle)

        // 4. Автозапуск при ввімкненні ТВ
        val btnBoot = createTvButton(
            if (prefs.autoStartOnBoot) "🚀 Автозапуск при ввімкненні ТВ: ТАК"
            else "🛑 Автозапуск при ввімкненні ТВ: НІ"
        )
        btnBoot.setOnClickListener {
            prefs.autoStartOnBoot = !prefs.autoStartOnBoot
            btnBoot.text = if (prefs.autoStartOnBoot) "🚀 Автозапуск при ввімкненні ТВ: ТАК"
            else "🛑 Автозапуск при ввімкненні ТВ: НІ"
        }
        root.addView(btnBoot)

        // Розділ будильників
        val alarmTitle = TextView(this).apply {
            text = "Мої будильники:"
            textSize = 22f
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 32, 0, 16)
        }
        root.addView(alarmTitle)

        val btnAddAlarm = createTvButton("➕ Додати новий будильник")
        btnAddAlarm.setOnClickListener { showAddAlarmDialog() }
        root.addView(btnAddAlarm)

        alarmsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }
        root.addView(alarmsContainer)

        renderAlarmsList()

        scrollView.addView(root)
        setContentView(scrollView)
        btnToggleClock.requestFocus()
    }

    // Створення кнопки з яскравою обводкою при виборі пультом ТВ
    private fun createTvButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(32, 24, 32, 24)
            isFocusable = true
            isFocusableInTouchMode = true

            val focusedDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#2563EB"))
                setStroke(6, Color.parseColor("#FACC15")) // Жовта рамка для пульта
                cornerRadius = 16f
            }
            val normalDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                setStroke(2, Color.parseColor("#334155"))
                cornerRadius = 16f
            }
            val states = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
                addState(intArrayOf(), normalDrawable)
            }
            background = states

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            layoutParams = params
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-arm alarms after returning from Android's exact-alarm access screen.
        AlarmScheduler.rescheduleAll(this)
    }

    private fun toggleClock(btn: Button) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Надайте дозвіл 'Поверх інших вікон' у налаштуваннях ТВ", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }

        val intent = Intent(this, ClockOverlayService::class.java)
        if (prefs.isClockActive) {
            stopService(intent)
            prefs.isClockActive = false
            btn.text = "⚪ Годинник: ВИМКНЕНО (Натисніть, щоб увімкнути)"
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
            prefs.isClockActive = true
            btn.text = "🟢 Годинник: УВІМКНЕНО (Натисніть, щоб вимкнути)"
        }
    }

    private fun restartOverlayIfActive() {
        if (prefs.isClockActive) {
            stopService(Intent(this, ClockOverlayService::class.java))
            val intent = Intent(this, ClockOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        }
    }

    // Зручне для пульта діалогове вікно створення будильника
    private fun showAddAlarmDialog() {
        var hour = 7
        var minute = 30

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val timeDisplay = TextView(this).apply {
            text = String.format("%02d:%02d", hour, minute)
            textSize = 42f
            setTextColor(Color.parseColor("#FACC15"))
            gravity = Gravity.CENTER
        }

        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        val btnHourPlus = createSmallControl("+ Год") {
            hour = (hour + 1) % 24
            timeDisplay.text = String.format("%02d:%02d", hour, minute)
        }
        val btnHourMinus = createSmallControl("- Год") {
            hour = if (hour - 1 < 0) 23 else hour - 1
            timeDisplay.text = String.format("%02d:%02d", hour, minute)
        }
        val btnMinPlus = createSmallControl("+ Хв") {
            minute = (minute + 5) % 60
            timeDisplay.text = String.format("%02d:%02d", hour, minute)
        }
        val btnMinMinus = createSmallControl("- Хв") {
            minute = if (minute - 5 < 0) 55 else minute - 5
            timeDisplay.text = String.format("%02d:%02d", hour, minute)
        }

        controlsRow.addView(btnHourMinus)
        controlsRow.addView(btnHourPlus)
        controlsRow.addView(btnMinMinus)
        controlsRow.addView(btnMinPlus)

        // Поле для назви (при натисканні OK відкриє екранну клавіатуру)
        val inputLabel = EditText(this).apply {
            hint = "Назва (натисніть OK для клавіатури)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 18f
            isFocusable = true
        }

        layout.addView(timeDisplay)
        layout.addView(controlsRow)
        layout.addView(inputLabel)

        AlertDialog.Builder(this)
            .setTitle("Новий будильник")
            .setView(layout)
            .setPositiveButton("Зберегти") { _, _ ->
                val list = prefs.getAlarms()
                var newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt().coerceAtLeast(1)
                while (list.any { it.id == newId }) {
                    newId = if (newId == Int.MAX_VALUE) 1 else newId + 1
                }
                val labelText = inputLabel.text.toString().ifBlank { "Будильник" }
                val newItem = AlarmItem(newId, hour, minute, labelText, true)
                list.add(newItem)
                prefs.saveAlarms(list)
                scheduleSystemAlarm(newItem)
                renderAlarmsList()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun createSmallControl(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun renderAlarmsList() {
        alarmsContainer.removeAllViews()
        val list = prefs.getAlarms()
        if (list.isEmpty()) {
            alarmsContainer.addView(TextView(this).apply {
text = "Список порожній"
setTextColor(Color.GRAY)
})
return
}
for (alarm in list) {
val btn = createTvButton(
"⏰ ${String.format("%02d:%02d", alarm.hour, alarm.minute)} — ${alarm.label} [Натисніть щоб видалити]"
)
btn.setOnClickListener {
cancelSystemAlarm(alarm)
list.remove(alarm)
prefs.saveAlarms(list)
renderAlarmsList()
Toast.makeText(this, "Будильник видалено", Toast.LENGTH_SHORT).show()
}
alarmsContainer.addView(btn)
}
}
private fun scheduleSystemAlarm(alarm: AlarmItem) {
    val exact = AlarmScheduler.schedule(this, alarm)
    if (!exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Toast.makeText(
            this,
            "Для точного спрацювання дозвольте будильники в налаштуваннях Android",
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
            // Some Android TV firmwares do not expose this settings screen.
        }
    }
}

private fun cancelSystemAlarm(alarm: AlarmItem) {
    AlarmScheduler.cancel(this, alarm)
}

}
