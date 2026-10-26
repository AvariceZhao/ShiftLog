package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WorkdayCalendarTest {
    private val settings = defaultSettings().copy(useWorkdayCalendar = true)

    @Test
    fun calendar_distinguishesWeekendsHolidaysAndMakeupWorkdays() {
        assertFalse(WorkdayCalendar.isWorkday(LocalDate.parse("2026-10-24"), settings))
        assertFalse(WorkdayCalendar.isWorkday(LocalDate.parse("2026-10-01"), settings))
        assertTrue(WorkdayCalendar.isWorkday(LocalDate.parse("2026-10-10"), settings))
        assertTrue(WorkdayCalendar.isWorkday(LocalDate.parse("2025-01-26"), settings))
        assertFalse(WorkdayCalendar.isWorkday(LocalDate.parse("2025-02-04"), settings))
        assertTrue(WorkdayCalendar.isWorkday(LocalDate.parse("2024-02-04"), settings))
        assertFalse(WorkdayCalendar.isWorkday(LocalDate.parse("2024-09-17"), settings))
        assertTrue(WorkdayCalendar.isWorkday(LocalDate.parse("2026-10-26"), settings))
    }

    @Test
    fun calendar_manualScheduleOverridesHolidaysAndSettings() {
        val holiday = LocalDate.parse("2026-10-01")
        val work = ClockRecord(holiday.toString(), null, null, daySchedule = DaySchedule.WORKDAY)
        assertTrue(WorkdayCalendar.isWorkday(holiday, settings, work))

        val weekday = LocalDate.parse("2026-10-26")
        val rest = ClockRecord(weekday.toString(), null, null, daySchedule = DaySchedule.REST_DAY)
        assertFalse(WorkdayCalendar.isWorkday(weekday, settings, rest))
        assertFalse(WorkdayCalendar.isWorkday(weekday, settings.copy(useWorkdayCalendar = false), rest))
        assertTrue(WorkdayCalendar.isWorkday(holiday, settings.copy(useWorkdayCalendar = false)))
    }

    @Test
    fun calendar_reportsMissingYearsForCrossYearCycles() {
        assertNull(WorkdayCalendar.coverageNotice(PayCycle(LocalDate.parse("2026-10-09"), LocalDate.parse("2026-11-08"))))
        val notice = WorkdayCalendar.coverageNotice(PayCycle(LocalDate.parse("2026-12-09"), LocalDate.parse("2027-01-08")))
        assertNotNull(notice)
        assertTrue(notice!!.contains("2027"))
    }

    @Test
    fun stats_restDayPunchesDoNotCountHoursAttendanceOrAbsence() {
        val records = listOf(
            completed("2026-10-09"),
            completed("2026-10-10"), // Make-up Saturday.
            completed("2026-10-11"), // Rest Sunday.
        )
        val cycle = PayCycle(LocalDate.parse("2026-10-09"), LocalDate.parse("2026-10-12"))
        val today = LocalDate.parse("2026-10-13")
        val now = today.atTime(12, 0)
        val stats = StatsCalculator.cycleStats(records, settings, cycle, today, now, TestZone)
        val effective = StatsCalculator.effectiveAttendanceProgress(records, settings, cycle, today, now, TestZone)

        assertEquals(2, stats.clockedDays)
        assertEquals(16.0, stats.totalHours, 0.001)
        assertEquals(2, stats.completeCount)
        assertEquals(1, stats.absentCount)
        assertEquals(2.0, effective.effectiveAttendance, 0.001)
        assertEquals(1.0, effective.absentCount, 0.001)
    }

    @Test
    fun stats_holidayPunchesExcludedUntilManuallyMarkedAsWorkday() {
        val date = LocalDate.parse("2026-10-01")
        val cycle = PayCycle(date, date)
        val today = date.plusDays(2)
        val now = today.atTime(12, 0)
        val record = completed(date.toString())
        val resting = StatsCalculator.cycleStats(listOf(record), settings, cycle, today, now, TestZone)
        assertEquals(0.0, resting.totalHours, 0.001)

        val working = listOf(record.copy(daySchedule = DaySchedule.WORKDAY))
        val stats = StatsCalculator.cycleStats(working, settings, cycle, today, now, TestZone)
        val effective = StatsCalculator.effectiveAttendanceProgress(working, settings, cycle, today, now, TestZone)
        assertEquals(8.0, stats.totalHours, 0.001)
        assertEquals(1.0, effective.effectiveAttendance, 0.001)
    }

    @Test
    fun stats_leaveAndRestOverridesExemptOnlyTheirOwnDates() {
        val records = listOf(
            ClockRecord("2026-10-12", null, null, isLeave = true),
            ClockRecord("2026-10-13", null, null, daySchedule = DaySchedule.REST_DAY),
        )
        val cycle = PayCycle(LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-14"))
        val today = LocalDate.parse("2026-10-15")
        val now = today.atTime(12, 0)
        val progress = StatsCalculator.effectiveAttendanceProgress(records, settings, cycle, today, now)
        assertEquals(0.0, progress.effectiveAttendance, 0.001)
        assertEquals(1, progress.leaveCount)
        assertEquals(1.0, progress.absentCount, 0.001)
    }

    @Test
    fun overnightAbsenceWaitsUntilNextDayShiftEndAndCalendarAgrees() {
        val shift = LocalDate.parse("2026-10-23") // Friday; shift ends on Saturday.
        val cycle = PayCycle(shift, shift.plusDays(1))
        val today = shift.plusDays(1)
        val beforeEnd = today.atTime(4, 59)
        val atEnd = today.atTime(5, 0)
        assertEquals(0.0, StatsCalculator.effectiveAttendanceProgress(emptyList(), settings, cycle, today, beforeEnd).absentCount, 0.001)
        assertEquals(0, StatsCalculator.cycleStats(emptyList(), settings, cycle, today, beforeEnd).absentCount)
        assertEquals(CycleDayStatus.PENDING, CycleDayStatusCalculator.statusForDate(shift, cycle, null, settings, today, beforeEnd))
        assertEquals(1.0, StatsCalculator.effectiveAttendanceProgress(emptyList(), settings, cycle, today, atEnd).absentCount, 0.001)
        assertEquals(CycleDayStatus.ABSENT, CycleDayStatusCalculator.statusForDate(shift, cycle, null, settings, today, atEnd))
        assertEquals(CycleDayStatus.REST, CycleDayStatusCalculator.statusForDate(today, cycle, null, settings, today, atEnd))
    }

    private fun completed(day: String): ClockRecord {
        val date = LocalDate.parse(day)
        val start = millisAt(date, LocalTime.of(7, 0))
        return ClockRecord(day, start, start + 8 * 3_600_000L)
    }
}
