package com.example.tvclock

import java.util.Calendar

/**
 * Calculates the next local occurrence of a daily alarm.
 * Calendar is used so daylight-saving transitions preserve the selected wall-clock time.
 */
object AlarmTimeCalculator {
    fun nextTriggerAt(nowMillis: Long, hour: Int, minute: Int): Long {
        require(hour in 0..23) { "hour must be between 0 and 23" }
        require(minute in 0..59) { "minute must be between 0 and 59" }

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }
}
