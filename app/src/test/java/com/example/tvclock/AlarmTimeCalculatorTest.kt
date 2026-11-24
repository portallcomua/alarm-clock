package com.example.tvclock

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmTimeCalculatorTest {

    @Test
    fun schedulesLaterTimeToday() {
        val now = localTime(2026, Calendar.NOVEMBER, 24, 6, 15)
        val expected = localTime(2026, Calendar.NOVEMBER, 24, 7, 30)
        assertEquals(expected, AlarmTimeCalculator.nextTriggerAt(now, 7, 30))
    }

    @Test
    fun schedulesTomorrowWhenAlarmTimeHasPassed() {
        val now = localTime(2026, Calendar.NOVEMBER, 24, 8, 0)
        val expected = localTime(2026, Calendar.NOVEMBER, 25, 7, 30)
        assertEquals(expected, AlarmTimeCalculator.nextTriggerAt(now, 7, 30))
    }

    @Test
    fun schedulesTomorrowWhenAlarmTimeIsExactlyNow() {
        val now = localTime(2026, Calendar.NOVEMBER, 24, 7, 30)
        val expected = localTime(2026, Calendar.NOVEMBER, 25, 7, 30)
        assertEquals(expected, AlarmTimeCalculator.nextTriggerAt(now, 7, 30))
    }

    private fun localTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
