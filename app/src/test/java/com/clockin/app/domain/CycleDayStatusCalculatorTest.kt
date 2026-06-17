package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CycleDayStatusCalculatorTest {
    private val settings = defaultSettings()
    private val cycle = PayCycle(date(2026, 6, 9), date(2026, 6, 15))

    @Test
    fun normal_completeShift_isNormal() {
        val shift = date(2026, 6, 10)
        val record = ClockRecord(
            shiftDate = shift.toShiftDateString(),
            clockInTime = millisAt(shift, java.time.LocalTime.of(6, 58)),
            clockOutTime = millisAt(shift.plusDays(1), java.time.LocalTime.of(5, 2)),
        )
        val status = CycleDayStatusCalculator.statusForDate(
            date = shift,
            cycle = cycle,
            record = record,
            settings = settings,
            today = shift,
            now = LocalDateTime.of(shift, java.time.LocalTime.of(23, 0)),
        )
        assertEquals(CycleDayStatus.NORMAL, status)
    }

    @Test
    fun absent_dayWithoutRecord_isAbsent() {
        val shift = date(2026, 6, 10)
        val status = CycleDayStatusCalculator.statusForDate(
            date = shift,
            cycle = cycle,
            record = null,
            settings = settings,
            today = shift.plusDays(1),
            now = LocalDateTime.of(shift.plusDays(1), java.time.LocalTime.of(12, 0)),
        )
        assertEquals(CycleDayStatus.ABSENT, status)
    }

    @Test
    fun csvImport_parsesExportedRow() {
        val csv = """
            日期,上班时间,下班时间,工时(小时),备注
            2026/06/16,07:02,04:58,21.93,迟到
        """.trimIndent()
        val records = BackupImporter.parseCsv(csv, settings, TestZone).getOrThrow()
        assertEquals(1, records.size)
        assertEquals("2026-06-16", records.first().shiftDate)
        assertTrue(records.first().clockInTime != null)
        assertTrue(records.first().clockOutTime != null)
    }
}
