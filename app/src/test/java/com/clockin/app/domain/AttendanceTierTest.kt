package com.clockin.app.domain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AttendanceTierTest {
    private val tiers = listOf(
        AttendanceTier(0f, 0.25f, 0.75f),
        AttendanceTier(6f, 0.5f, 0.5f),
        AttendanceTier(8f, 1f, 0f),
    )

    @Test
    fun statistics_addsTierAbsenceToWholeDayAbsenceUsingSameBoundaryAsAttendance() {
        val settings = defaultSettings().copy(useWorkdayCalendar = false, attendanceTiers = tiers)
        val cycle = PayCycle(date(2026, 6, 9), date(2026, 6, 15))
        val records = listOf(
            completed(date(2026, 6, 9), 5.99),
            completed(date(2026, 6, 10), 6.0),
            completed(date(2026, 6, 11), 7.99),
            completed(date(2026, 6, 12), 8.0),
            ClockRecord("2026-06-13", null, null, isLeave = true),
            // June 14 has no punches and counts as one full absence.
            ClockRecord("2026-06-15", millisAt(date(2026, 6, 15), LocalTime.of(7, 0)), null),
        )
        val today = date(2026, 6, 16)
        val progress = StatsCalculator.effectiveAttendanceProgress(
            records, settings, cycle, today, today.atTime(12, 0), TestZone,
        )
        assertEquals(2.5, progress.effectiveAttendance, 0.001)
        assertEquals(3.5, progress.absentCount, 0.001)
        assertEquals(1, progress.leaveCount)

        val revised = StatsCalculator.effectiveAttendanceProgress(
            records, settings.copy(attendanceTiers = tiers.map { it.copy(absenceCredit = 0f) }),
            cycle, today, today.atTime(12, 0), TestZone,
        )
        assertEquals(2.5, revised.effectiveAttendance, 0.001)
        assertEquals(1.0, revised.absentCount, 0.001)
    }

    @Test
    fun statistics_attendanceAndAbsenceCreditsAreIndependent() {
        val day = date(2026, 6, 10)
        val settings = defaultSettings().copy(attendanceTiers = listOf(AttendanceTier(0f, 0.3f, 0.2f)))
        val progress = StatsCalculator.effectiveAttendanceProgress(
            listOf(completed(day, 2.0)), settings, PayCycle(day, day), day.plusDays(2),
            day.plusDays(2).atTime(12, 0), TestZone,
        )
        assertEquals(0.3, progress.effectiveAttendance, 0.001)
        assertEquals(0.2, progress.absentCount, 0.001)
        assertEquals("0.2", StatsCalculator.formatAttendance(progress.absentCount))
    }

    @Test
    fun statistics_neverAppliesTierAbsenceToLeaveWeekendsOrHolidays() {
        val settings = defaultSettings().copy(attendanceTiers = listOf(AttendanceTier(0f, 0.5f, 2f)))
        val records = listOf(
            completed(date(2026, 6, 12), 3.0).copy(isLeave = true),
            completed(date(2026, 6, 13), 3.0),
            completed(date(2026, 6, 19), 3.0),
        )
        records.forEach { record ->
            val day = LocalDate.parse(record.shiftDate)
            val progress = StatsCalculator.effectiveAttendanceProgress(
                listOf(record), settings, PayCycle(day, day), day.plusDays(2),
                day.plusDays(2).atTime(12, 0), TestZone,
            )
            assertEquals(0.0, progress.effectiveAttendance, 0.001)
            assertEquals(0.0, progress.absentCount, 0.001)
        }
    }

    @Test
    fun preferences_oldTwoFieldTiersKeepZeroAbsenceAndNewTiersRoundTrip() {
        assertEquals(
            listOf(AttendanceTier(0f, 0.5f, 0f), AttendanceTier(6f, 0.5f, 0f), AttendanceTier(8f, 1f, 0f)),
            AttendanceTierCodec.decode("0.0:0.5;6.0:0.5;8.0:1.0"),
        )
        assertEquals(tiers, AttendanceTierCodec.decode(AttendanceTierCodec.encode(tiers.reversed())))
        assertTrue(AttendanceTierCodec.decode("0:0.5:0.5;6:0.5;8:1:0").all { it.isValid })
    }

    @Test
    fun defaults_shortShiftIsHalfAbsenceAndSixHoursStartsHalfAttendance() {
        val day = date(2026, 6, 10)
        val settings = defaultSettings()
        listOf(
            Triple(0.0, 0.0, 0.5),
            Triple(5.99, 0.0, 0.5),
            Triple(6.0, 0.5, 0.0),
            Triple(7.99, 0.5, 0.0),
            Triple(8.0, 1.0, 0.0),
        ).forEach { (hours, attendance, absence) ->
            val progress = StatsCalculator.effectiveAttendanceProgress(
                listOf(completed(day, hours)), settings, PayCycle(day, day), day.plusDays(2),
                day.plusDays(2).atTime(12, 0), TestZone,
            )
            assertEquals("attendance for $hours hours", attendance, progress.effectiveAttendance, 0.001)
            assertEquals("absence for $hours hours", absence, progress.absentCount, 0.001)
        }
        assertEquals(DefaultAttendanceTiers, AttendanceTierCodec.decode(null))
    }

    @Test
    fun preferences_invalidAbsenceNeverCreatesNegativeOrNonFiniteTotals() {
        assertEquals(
            listOf(AttendanceTier(8f, 1f, 0f)),
            AttendanceTierCodec.decode("0:0.5:-1;1:0.5:NaN;2:0.5:Infinity;8:1:0"),
        )
        assertEquals(DefaultAttendanceTiers, AttendanceTierCodec.decode("0:0.5:invalid"))
    }

    @Test
    fun backups_preserveBothCreditsAndOldBackupsDefaultToZeroAbsence() {
        val json = BackupExporter.export(defaultSettings().copy(attendanceTiers = tiers), emptyList())
        assertEquals(tiers, BackupImporter.parseJson(json).getOrThrow().settings.attendanceTiers)
        val old = JSONObject(json)
        val array = old.getJSONObject("settings").getJSONArray("attendanceTiers")
        for (i in 0 until array.length()) array.getJSONObject(i).remove("absenceCredit")
        assertEquals(
            tiers.map { it.copy(absenceCredit = 0f) },
            BackupImporter.parseJson(old.toString()).getOrThrow().settings.attendanceTiers,
        )
    }

    private fun completed(day: LocalDate, hours: Double): ClockRecord {
        val start = millisAt(day, LocalTime.of(7, 0))
        return ClockRecord(day.toString(), start, start + (hours * 3_600_000).toLong())
    }
}
