package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ShiftCalculatorTest {
    private val settings = defaultSettings()

    @Test
    fun shiftDateForClockIn_usesCalendarDate() {
        val ms = millisAt(date(2026, 6, 16), LocalTime.of(7, 2))
        assertEquals("2026-06-16", ShiftCalculator.shiftDateForClockIn(ms, TestZone))
    }

    @Test
    fun shiftDateForClockOut_beforeStandardTime_goesToPreviousShift() {
        val ms = millisAt(date(2026, 6, 17), LocalTime.of(4, 58))
        assertEquals("2026-06-16", ShiftCalculator.shiftDateForClockOut(ms, settings, TestZone))
    }

    @Test
    fun shiftDateForClockOut_afterStandardTime_staysOnSameDay() {
        val ms = millisAt(date(2026, 6, 18), LocalTime.of(5, 30))
        assertEquals("2026-06-18", ShiftCalculator.shiftDateForClockOut(ms, settings, TestZone))
    }

    @Test
    fun currentShiftDate_before5am_isPreviousCalendarDay() {
        val now = LocalDateTime.of(2026, 6, 17, 4, 30)
        assertEquals("2026-06-16", ShiftCalculator.currentShiftDate(now, settings))
    }

    @Test
    fun resolveActiveShiftDate_prefersOpenShiftWithoutClockOut() {
        val open = ClockRecord("2026-06-15", clockInTime = 1L, clockOutTime = null)
        val now = LocalDateTime.of(2026, 6, 17, 7, 0)
        assertEquals("2026-06-15", ShiftCalculator.resolveActiveShiftDate(now, settings, open))
    }

    @Test
    fun clockInStatus_detectsLateAndNormal() {
        val onTime = millisAt(date(2026, 6, 16), LocalTime.of(7, 0))
        val late = millisAt(date(2026, 6, 16), LocalTime.of(7, 1))
        assertEquals(PunchStatus.NORMAL, ShiftCalculator.clockInStatus(onTime, settings, TestZone))
        assertEquals(PunchStatus.LATE, ShiftCalculator.clockInStatus(late, settings, TestZone))
    }

    @Test
    fun clockOutStatus_detectsEarlyNormalAndMissed() {
        val shiftDate = "2026-06-16"
        val early = millisAt(date(2026, 6, 17), LocalTime.of(4, 30))
        val normal = millisAt(date(2026, 6, 17), LocalTime.of(5, 0))
        assertEquals(PunchStatus.EARLY, ShiftCalculator.clockOutStatus(early, 1L, shiftDate, settings, TestZone))
        assertEquals(PunchStatus.NORMAL, ShiftCalculator.clockOutStatus(normal, 1L, shiftDate, settings, TestZone))
        assertEquals(PunchStatus.MISSED_OUT, ShiftCalculator.clockOutStatus(null, 1L, shiftDate, settings, TestZone))
    }

    @Test
    fun clockOutStatus_sameDayEvening_isEarlyForNightShift() {
        val shiftDate = "2026-06-16"
        val evening = millisAt(date(2026, 6, 16), LocalTime.of(22, 0))
        assertEquals(PunchStatus.EARLY, ShiftCalculator.clockOutStatus(evening, 1L, shiftDate, settings, TestZone))
    }

    @Test
    fun hoursWorked_calculatesFromMillisDifference() {
        val inMs = millisAt(date(2026, 6, 16), LocalTime.of(7, 0))
        val outMs = millisAt(date(2026, 6, 17), LocalTime.of(4, 58))
        val hours = ShiftCalculator.hoursWorked(inMs, outMs)
        assertEquals(21.97, hours, 0.01)
    }
}
