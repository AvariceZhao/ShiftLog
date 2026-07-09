package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ExpectedClockOutCalculatorTest {
    private val progress = TargetProgress(
        targetDays = 21,
        targetHours = 160f,
        clockedDays = 8,
        totalHours = 80.0,
        remainingDays = 13,
        remainingHours = 80.0,
        currentDailyAvg = 10.0,
        cycleDaysRemaining = 20,
        requiredDailyByTargetDays = 80.0 / 13,
        requiredDailyByTargetDaysUnreachable = false,
        requiredDailyByCycleDays = 80.0 / 20,
        requiredDailyByCycleDaysUnreachable = false,
        daysTargetMetHoursNotMet = false,
    )

    @Test
    fun calculate_returnsNullWhenAlreadyClockedOut() {
        val clockIn = millisAt(date(2026, 6, 10), LocalTime.of(7, 0))
        val clockOut = millisAt(date(2026, 6, 11), LocalTime.of(5, 0))
        assertNull(ExpectedClockOutCalculator.calculate(clockIn, clockOut, progress, TestZone))
    }

    @Test
    fun calculate_returnsNullWhenTargetMet() {
        val met = progress.copy(remainingHours = 0.0, remainingDays = 0)
        val clockIn = millisAt(date(2026, 6, 10), LocalTime.of(7, 0))
        assertNull(ExpectedClockOutCalculator.calculate(clockIn, null, met, TestZone))
    }

    @Test
    fun calculate_showsBothHintsForOpenShift() {
        val clockIn = millisAt(date(2026, 6, 10), LocalTime.of(7, 0))
        val result = ExpectedClockOutCalculator.calculate(clockIn, null, progress, TestZone)
        assertNotNull(result)
        assertNotNull(result!!.byTargetDays)
        assertNotNull(result.byCycleDays)
        assertEquals("按目标出勤", result.byTargetDays!!.label)
        assertEquals("按周期日历", result.byCycleDays!!.label)
    }

    @Test
    fun formatExpectedClockOut_sameDay() {
        val clockIn = millisAt(date(2026, 6, 10), LocalTime.of(7, 0))
        assertEquals(
            "15:09",
            ExpectedClockOutCalculator.formatExpectedClockOut(clockIn, 8.15, TestZone),
        )
    }

    @Test
    fun formatExpectedClockOut_crossesMidnight() {
        val clockIn = millisAt(date(2026, 6, 10), LocalTime.of(7, 0))
        val text = ExpectedClockOutCalculator.formatExpectedClockOut(clockIn, 22.0, TestZone)
        assertEquals("6/11 05:00", text)
    }

    @Test
    fun calculate_marksUnreachableWhenDailyAbove24Hours() {
        val heavy = progress.copy(
            remainingDays = 1,
            remainingHours = 30.0,
            requiredDailyByTargetDays = 30.0,
            requiredDailyByTargetDaysUnreachable = true,
        )
        val clockIn = millisAt(date(2026, 6, 10), LocalTime.of(7, 0))
        val result = ExpectedClockOutCalculator.calculate(clockIn, null, heavy, TestZone)
        assertNotNull(result?.byTargetDays)
        assertTrue(result!!.byTargetDays!!.unreachable)
        assertEquals("无法达成", result.byTargetDays!!.timeText)
    }
}
