package com.clockin.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    private val settings = defaultSettings()

    @Test
    fun export_usesExcelSafeSlashDates_andIncludesAllRecords() {
        val clockIn = millisAt(date(2026, 6, 16), java.time.LocalTime.of(7, 2))
        val clockOut = millisAt(date(2026, 6, 17), java.time.LocalTime.of(4, 58))
        val csv = CsvExporter.export(
            records = listOf(
                ClockRecord(
                    shiftDate = "2026-06-16",
                    clockInTime = clockIn,
                    clockOutTime = clockOut,
                ),
                ClockRecord(
                    shiftDate = "2026-05-01",
                    clockInTime = millisAt(date(2026, 5, 1), java.time.LocalTime.of(7, 0)),
                    clockOutTime = millisAt(date(2026, 5, 2), java.time.LocalTime.of(5, 0)),
                ),
            ),
            settings = settings,
        )

        assertTrue(csv.contains("# 范围,全部周期"))
        assertTrue(csv.contains("# 记录数,2"))
        assertTrue(csv.contains("2026/06/16,07:02,04:58,21.93,迟到"))
        assertTrue(csv.contains("2026/05/01,07:00,05:00"))
        assertFalse(csv.contains("2026-06-16,07:02"))
    }
}
