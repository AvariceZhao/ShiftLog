package com.clockin.app.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkdayBackupTest {
    @Test
    fun json_preservesScheduleOverridesAndCalendarSetting() {
        val settings = defaultSettings().copy(useWorkdayCalendar = false)
        val records = listOf(
            ClockRecord("2026-10-10", null, null, daySchedule = DaySchedule.REST_DAY),
            ClockRecord("2026-10-11", null, null, daySchedule = DaySchedule.WORKDAY),
            ClockRecord("2026-10-12", null, null, isLeave = true),
        )
        val restored = BackupImporter.parseJson(BackupExporter.export(settings, records)).getOrThrow()
        assertFalse(restored.settings.useWorkdayCalendar)
        assertEquals(records, restored.records)
    }

    @Test
    fun oldJson_defaultsToCalendarAndAutomaticSchedule() {
        val json = JSONObject(BackupExporter.export(
            defaultSettings(),
            listOf(ClockRecord("2026-10-12", null, null)),
        ))
        json.getJSONObject("settings").remove("useWorkdayCalendar")
        json.getJSONArray("records").getJSONObject(0).remove("daySchedule")
        val restored = BackupImporter.parseJson(json.toString()).getOrThrow()
        assertTrue(restored.settings.useWorkdayCalendar)
        assertEquals(DaySchedule.AUTO, restored.records.single().daySchedule)
    }

    @Test
    fun csv_keepsScheduleOnlyRecordsAndExcludesRestHoursFromTotals() {
        val settings = defaultSettings()
        val start = millisAt(date(2026, 10, 11), java.time.LocalTime.of(7, 0))
        val records = listOf(
            ClockRecord("2026-10-11", start, start + 8 * 3_600_000L),
            ClockRecord("2026-10-12", null, null, daySchedule = DaySchedule.REST_DAY),
            ClockRecord("2026-10-13", null, null, daySchedule = DaySchedule.WORKDAY),
        )
        val csv = CsvExporter.export(records, settings)
        assertTrue(csv.contains("# 合计,0天,0.0h"))
        assertTrue(!csv.contains("上课时长"))
        assertTrue(csv.contains("2026/10/11,07:00,15:00,,休息,跟随日历"))
        assertEquals(records, BackupImporter.parseCsv(csv, settings, TestZone).getOrThrow().sortedBy { it.shiftDate })
    }

    @Test
    fun courseMinutes_surviveJsonAndCsvIncludingCourseOnlyDays() {
        val records = listOf(
            ClockRecord("2026-10-12", null, null, courseMinutes = 360),
            ClockRecord("2026-10-13", null, null, daySchedule = DaySchedule.REST_DAY, courseMinutes = 120),
        )
        val settings = defaultSettings()
        assertEquals(records, BackupImporter.parseJson(BackupExporter.export(settings, records)).getOrThrow().records)
        val csv = CsvExporter.export(records, settings)
        assertTrue(csv.contains("2026/10/12,,,,缺卡,跟随日历,360,360,360") ||
            csv.contains("2026/10/12,,,,,跟随日历,360,360,360"))
        assertEquals(records, BackupImporter.parseCsv(csv, settings, TestZone).getOrThrow().sortedBy { it.shiftDate })
    }

    @Test
    fun oldBackupWithoutCourseMinutes_defaultsToZero() {
        val json = JSONObject(BackupExporter.export(defaultSettings(), listOf(ClockRecord("2026-10-12", null, null))))
        json.getJSONArray("records").getJSONObject(0).remove("courseMinutes")
        assertEquals(0, BackupImporter.parseJson(json.toString()).getOrThrow().records.single().courseMinutes)
    }
}
