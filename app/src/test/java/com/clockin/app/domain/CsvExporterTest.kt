package com.clockin.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    private val settings = defaultSettings()
    private val cycle = PayCycle(date(2026, 6, 9), date(2026, 7, 8))
    private val progress = TargetProgress(
        targetDays = 21,
        targetHours = 160f,
        clockedDays = 1,
        totalHours = 21.93,
        remainingDays = 20,
        remainingHours = 138.07,
        currentDailyAvg = 21.93,
        cycleDaysRemaining = 30,
        requiredDailyByTargetDays = 6.9,
        requiredDailyByTargetDaysUnreachable = false,
        requiredDailyByCycleDays = 4.6,
        requiredDailyByCycleDaysUnreachable = false,
        daysTargetMetHoursNotMet = false,
    )

    @Test
    fun export_usesExcelSafeSlashDates_notMinusDates() {
        val clockIn = millisAt(date(2026, 6, 16), java.time.LocalTime.of(7, 2))
        val clockOut = millisAt(date(2026, 6, 17), java.time.LocalTime.of(4, 58))
        val csv = CsvExporter.export(
            cycle = cycle,
            records = listOf(
                ClockRecord(
                    shiftDate = "2026-06-16",
                    clockInTime = clockIn,
                    clockOutTime = clockOut,
                ),
            ),
            settings = settings,
            progress = progress,
        )

        assertTrue(csv.contains("# 周期,2026/06/09,2026/07/08"))
        assertTrue(csv.contains("2026/06/16,07:02,04:58,21.93,迟到"))
        assertFalse(csv.contains("2026-06-16,07:02"))
    }
}
