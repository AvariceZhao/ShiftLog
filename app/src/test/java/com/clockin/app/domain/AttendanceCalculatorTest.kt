package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AttendanceCalculatorTest {
    private val settings = defaultSettings().copy(
        standardClockIn = LocalTime.of(9, 0),
        standardClockOut = LocalTime.of(17, 0),
        isClockOutNextDay = false,
    )

    @Test
    fun octoberReport_usesEighteenWorkdaysAnd8640ExpectedMinutes() {
        val cycle = PayCycle(date(2026, 10, 1), date(2026, 10, 31))
        val stats = calculate(emptyList(), cycle)
        assertEquals(18, stats.workdays)
        assertEquals(13, stats.restDays)
        assertEquals(18, stats.fullAbsenceDays)
        assertEquals(8640.0, stats.expectedMinutes, 0.001)
        assertEquals(0.0, stats.attendanceRate!!, 0.001)
        assertEquals(18, stats.settledWorkdays)
        assertEquals(0, stats.remainingWorkdays)
    }

    @Test
    fun dailyCap_isAppliedBeforeSummingAndDoesNotConvertOvertimeIntoAnotherDaysAttendance() {
        val first = date(2026, 10, 8)
        val second = first.plusDays(1)
        val stats = calculate(listOf(completed(first, 600), completed(second, 360)), PayCycle(first, second))
        assertEquals(960.0, stats.punchMinutes, 0.001)
        assertEquals(960.0, stats.effectiveMinutes, 0.001)
        assertEquals(840.0, stats.creditedMinutes, 0.001)
        assertEquals(87.5, stats.attendanceRate!!, 0.001)
        assertEquals(120.0, stats.remainingMinutes, 0.001)
        assertEquals(1.5, stats.effectiveAttendance, 0.001)
        assertEquals(1, stats.fullAttendanceDays)
        assertEquals(1, stats.halfAttendanceDays)
    }

    @Test
    fun courseMinutes_addToPunchSpanBeforeBothTierMatchingAndCapping() {
        val day = date(2026, 10, 8)
        val record = completed(day, 300).copy(courseMinutes = 240)
        val stats = calculate(listOf(record), PayCycle(day, day))
        assertEquals(300.0, stats.punchMinutes, 0.001)
        assertEquals(240.0, stats.courseMinutes, 0.001)
        assertEquals(540.0, stats.effectiveMinutes, 0.001)
        assertEquals(480.0, stats.creditedMinutes, 0.001)
        assertEquals(100.0, stats.attendanceRate!!, 0.001)
        assertEquals(1.0, stats.effectiveAttendance, 0.001)
        assertEquals(1, stats.fullAttendanceDays)
    }

    @Test
    fun courseOnlyDay_isNotAbsentAndDoesNotInventPunches() {
        val day = date(2026, 10, 8)
        val record = ClockRecord(day.toString(), null, null, courseMinutes = 360)
        val cycle = PayCycle(day, day)
        val stats = calculate(listOf(record), cycle)
        assertEquals(0, stats.clockedDays)
        assertEquals(0.0, stats.punchMinutes, 0.001)
        assertEquals(360.0, stats.creditedMinutes, 0.001)
        assertEquals(75.0, stats.attendanceRate!!, 0.001)
        assertEquals(0.5, stats.effectiveAttendance, 0.001)
        assertEquals(0, stats.fullAbsenceDays)
        assertEquals(1, stats.halfAttendanceDays)
        assertEquals(CycleDayStatus.NORMAL, CycleDayStatusCalculator.statusForDate(
            day, cycle, record, settings, day.plusDays(1), day.plusDays(1).atTime(12, 0),
        ))
    }

    @Test
    fun distribution_andCustomTierCreditsRemainSeparateAndAllDaysReconcile() {
        val start = date(2026, 10, 8)
        val cycle = PayCycle(start, start.plusDays(3))
        val allDays = settings.copy(useWorkdayCalendar = false, attendanceTiers = listOf(
            AttendanceTier(0f, 0.2f, 0.7f), AttendanceTier(6f, 0.6f, 0.3f), AttendanceTier(8f, 1.2f, 0.1f),
        ))
        val stats = calculate(listOf(
            completed(start, 359), completed(start.plusDays(1), 360), completed(start.plusDays(2), 480),
        ), cycle, allDays)
        assertEquals(1, stats.shortAttendanceDays)
        assertEquals(1, stats.halfAttendanceDays)
        assertEquals(1, stats.fullAttendanceDays)
        assertEquals(1, stats.fullAbsenceDays)
        assertEquals(2.0, stats.effectiveAttendance, 0.001)
        assertEquals(2.1, stats.absentCount, 0.001)
        assertEquals(listOf(1, 1, 1), stats.tierBreakdown.map { it.days })
        assertEquals(stats.workdays, stats.fullAttendanceDays + stats.halfAttendanceDays +
            stats.shortAttendanceDays + stats.fullAbsenceDays + stats.leaveCount + stats.pendingDays)
    }

    @Test
    fun fullCycleDenominatorIncludesFutureWorkdaysButTheyArePendingRatherThanAbsent() {
        val start = date(2026, 10, 12)
        val cycle = PayCycle(start, start.plusDays(4))
        val stats = AttendanceCalculator.calculate(listOf(
            completed(start, 480), completed(start.plusDays(1), 480),
            completed(start.plusDays(4), 480), // Future record must not increase the numerator.
        ), settings, cycle, start.plusDays(1), start.plusDays(1).atTime(18, 0))
        assertEquals(5, stats.workdays)
        assertEquals(2, stats.settledWorkdays)
        assertEquals(3, stats.remainingWorkdays)
        assertEquals(3, stats.pendingDays)
        assertEquals(0, stats.fullAbsenceDays)
        assertEquals(960.0, stats.creditedMinutes, 0.001)
        assertEquals(40.0, stats.attendanceRate!!, 0.001)
        assertEquals(2, stats.currentWorkdays)
        assertEquals(100.0, stats.currentAttendanceRate!!, 0.001)
    }

    @Test
    fun leaveExemptsCreditsAndAbsenceButKeepsWorkdayDenominator() {
        val day = date(2026, 10, 8)
        val record = completed(day, 480).copy(isLeave = true, courseMinutes = 180)
        val stats = calculate(listOf(record), PayCycle(day, day))
        assertEquals(1, stats.leaveCount)
        assertEquals(1, stats.workdays)
        assertEquals(480.0, stats.expectedMinutes, 0.001)
        assertEquals(0.0, stats.effectiveMinutes, 0.001)
        assertEquals(0.0, stats.absentCount, 0.001)
        assertEquals(0.0, stats.effectiveAttendance, 0.001)
        assertEquals(0.0, stats.attendanceRate!!, 0.001)
    }

    @Test
    fun restDaysExcludeCourseAndPunchMinutesButManualWorkdayRestoresThem() {
        val day = date(2026, 10, 11)
        val cycle = PayCycle(day, day)
        val record = completed(day, 300).copy(courseMinutes = 180)
        val rest = calculate(listOf(record), cycle)
        assertEquals(1, rest.restDays)
        assertEquals(0, rest.workdays)
        assertEquals(0.0, rest.effectiveMinutes, 0.001)
        assertNull(rest.attendanceRate)
        val work = calculate(listOf(record.copy(daySchedule = DaySchedule.WORKDAY)), cycle)
        assertEquals(1, work.workdays)
        assertEquals(480.0, work.effectiveMinutes, 0.001)
        assertEquals(100.0, work.attendanceRate!!, 0.001)
    }

    @Test
    fun openOvernightShift_staysPendingUntilEndThenUsesZeroPunchSpanAndCourseMinutes() {
        val day = date(2026, 10, 23)
        val cycle = PayCycle(day, day.plusDays(1))
        val overnight = defaultSettings()
        val record = ClockRecord(day.toString(), millisAt(day, LocalTime.of(7, 0)), null)
        val before = AttendanceCalculator.calculate(listOf(record), overnight, cycle, day.plusDays(1), day.plusDays(1).atTime(4, 59))
        assertEquals(1, before.pendingDays)
        assertEquals(0, before.shortAttendanceDays)
        assertEquals(0.0, before.absentCount, 0.001)

        val after = AttendanceCalculator.calculate(listOf(record), overnight, cycle, day.plusDays(1), day.plusDays(1).atTime(5, 0))
        assertEquals(0, after.pendingDays)
        assertEquals(1, after.incompletePunchDays)
        assertEquals(1, after.shortAttendanceDays)
        assertEquals(0, after.fullAbsenceDays)
        assertEquals(0.5, after.absentCount, 0.001)

        val withCourse = calculate(listOf(record.copy(courseMinutes = 360)), PayCycle(day, day), overnight)
        assertEquals(0.0, withCourse.punchMinutes, 0.001)
        assertEquals(360.0, withCourse.courseMinutes, 0.001)
        assertEquals(0.5, withCourse.effectiveAttendance, 0.001)
    }

    @Test
    fun recordsOutsideCycleAreIgnoredAndEmptyWorkdayCycleHasNoRate() {
        val day = date(2026, 10, 11)
        val stats = calculate(listOf(completed(day.plusDays(1), 480)), PayCycle(day, day))
        assertEquals(0, stats.clockedDays)
        assertEquals(0.0, stats.creditedMinutes, 0.001)
        assertNull(stats.attendanceRate)
        assertTrue(stats.tierBreakdown.all { it.days == 0 })
    }

    @Test
    fun daysBelowFirstCustomThresholdStillCountHoursAndAreFlaggedAsUnmatched() {
        val day = date(2026, 10, 8)
        val stats = calculate(listOf(completed(day, 120)), PayCycle(day, day),
            settings.copy(attendanceTiers = listOf(AttendanceTier(8f, 1f))))
        assertEquals(120.0, stats.creditedMinutes, 0.001)
        assertEquals(25.0, stats.attendanceRate!!, 0.001)
        assertEquals(1, stats.unmatchedTierDays)
        assertEquals(1, stats.shortAttendanceDays)
        assertEquals(0.0, stats.effectiveAttendance, 0.001)
    }

    @Test
    fun currentRate_completedEarlyShiftJoinsDenominatorWithoutWaitingForScheduledEnd() {
        val day = date(2026, 10, 12)
        val stats = AttendanceCalculator.calculate(
            listOf(completed(day, 420)), settings, PayCycle(day, day.plusDays(4)),
            day, day.atTime(16, 0),
        )
        assertEquals(0, stats.settledWorkdays)
        assertEquals(1, stats.currentWorkdays)
        assertEquals(480.0, stats.currentExpectedMinutes, 0.001)
        assertEquals(87.5, stats.currentAttendanceRate!!, 0.001)
    }

    @Test
    fun currentRate_openShiftDoesNotReducePreviouslyCompletedDaysRate() {
        val first = date(2026, 10, 12)
        val today = first.plusDays(1)
        val open = ClockRecord(today.toString(), millisAt(today, LocalTime.of(9, 0)), null)
        val stats = AttendanceCalculator.calculate(
            listOf(completed(first, 480), open), settings, PayCycle(first, first.plusDays(4)),
            today, today.atTime(12, 0),
        )
        assertEquals(1, stats.currentWorkdays)
        assertEquals(100.0, stats.currentAttendanceRate!!, 0.001)
        assertEquals(0.0, stats.absentCount, 0.001)
    }

    @Test
    fun currentRate_emptyDayIsUndefinedBeforeShiftEndAndZeroAfterEnd() {
        val day = date(2026, 10, 12)
        val cycle = PayCycle(day, day.plusDays(4))
        val before = AttendanceCalculator.calculate(emptyList(), settings, cycle, day, day.atTime(16, 59))
        assertEquals(0, before.currentWorkdays)
        assertNull(before.currentAttendanceRate)
        val after = AttendanceCalculator.calculate(emptyList(), settings, cycle, day, day.atTime(17, 0))
        assertEquals(1, after.currentWorkdays)
        assertEquals(0.0, after.currentAttendanceRate!!, 0.001)
    }

    @Test
    fun currentRate_courseOnlyDayUsesSameDenominatorAsCompletedPunchDay() {
        val day = date(2026, 10, 12)
        val record = ClockRecord(day.toString(), null, null, courseMinutes = 180)
        val stats = AttendanceCalculator.calculate(
            listOf(record), settings, PayCycle(day, day.plusDays(4)), day, day.atTime(12, 0),
        )
        assertEquals(1, stats.currentWorkdays)
        assertEquals(37.5, stats.currentAttendanceRate!!, 0.001)
    }

    @Test
    fun durationFormatting_keepsWholeMinutesAndCarriesHoursWithoutRoundingUp() {
        assertEquals("0h0m", StatsCalculator.formatAttendanceDuration(0.0))
        assertEquals("0h59m", StatsCalculator.formatAttendanceDuration(59.9))
        assertEquals("1h0m", StatsCalculator.formatAttendanceDuration(60.0))
        assertEquals("7h59m", StatsCalculator.formatAttendanceDuration(479.99))
        assertEquals("144h30m", StatsCalculator.formatAttendanceDuration(8670.0))
    }

    private fun completed(day: LocalDate, minutes: Int): ClockRecord {
        val start = millisAt(day, LocalTime.of(9, 0))
        return ClockRecord(day.toString(), start, start + minutes * 60_000L)
    }

    private fun calculate(
        records: List<ClockRecord>,
        cycle: PayCycle,
        config: AppSettings = settings,
    ) = AttendanceCalculator.calculate(records, config, cycle, date(2026, 11, 2), LocalDateTime.of(2026, 11, 2, 12, 0))
}
