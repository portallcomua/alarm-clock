package com.example.tvclock

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class AlarmItem(
    val id: Int,
    var hour: Int,
    var minute: Int,
    var label: String,
    var isEnabled: Boolean
)

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("tv_clock_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var isFullScreen: Boolean
        get() = prefs.getBoolean("full_screen", false)
        set(value) = prefs.edit().putBoolean("full_screen", value).apply()

    var isAnalog: Boolean
        get() = prefs.getBoolean("is_analog", false)
        set(value) = prefs.edit().putBoolean("is_analog", value).apply()

    var autoStartOnBoot: Boolean
        get() = prefs.getBoolean("auto_start", false)
        set(value) = prefs.edit().putBoolean("auto_start", value).apply()

    var isClockActive: Boolean
        get() = prefs.getBoolean("clock_active", false)
        set(value) = prefs.edit().putBoolean("clock_active", value).apply()

    fun getAlarms(): MutableList<AlarmItem> {
        val json = prefs.getString("alarms_list", null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
            gson.fromJson<MutableList<AlarmItem>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveAlarms(list: List<AlarmItem>) {
        prefs.edit().putString("alarms_list", gson.toJson(list)).apply()
    }
}
