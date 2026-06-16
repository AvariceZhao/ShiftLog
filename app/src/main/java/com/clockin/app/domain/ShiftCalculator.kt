package com.clockin.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ShiftCalculator {
    fun shiftDateForClockIn(instantMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return instantMs.toLocalDateTime(zoneId).toLocalDate().toShiftDateString()
    }

    fun shiftDateForClockOut(instantMs: Long, settings: AppSettings, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val dateTime = instantMs.toLocalDateTime(zoneId)
        val time = dateTime.toLocalTime()
        val calendarDate = dateTime.toLocalDate()
        return if (settings.isClockOutNextDay && !time.isAfter(settings.standardClockOut)) {
            calendarDate.minusDays(1).toShiftDateString()
        } else {
            calendarDate.toShiftDateString()
        }
    }

    /** 当前应操作的班次日期（用于首页打卡状态） */
    fun currentShiftDate(now: LocalDateTime, settings: AppSettings): String {
        val time = now.toLocalTime()
        val date = now.toLocalDate()
        return if (settings.isClockOutNextDay && !time.isAfter(settings.standardClockOut)) {
            date.minusDays(1).toShiftDateString()
        } else {
            date.toShiftDateString()
        }
    }

    fun resolveActiveShiftDate(
        now: LocalDateTime,
        settings: AppSettings,
        openShift: ClockRecord?,
    ): String {
        if (openShift != null && openShift.clockInTime != null && openShift.clockOutTime == null) {
            return openShift.shiftDate
        }
        return currentShiftDate(now, settings)
    }

    fun hoursWorked(clockIn: Long, clockOut: Long): Double {
        val millis = (clockOut - clockIn).coerceAtLeast(0)
        return millis / 3_600_000.0
    }

    fun clockInStatus(clockInMs: Long?, settings: AppSettings, zoneId: ZoneId = ZoneId.systemDefault()): PunchStatus {
        if (clockInMs == null) return PunchStatus.MISSED_IN
        val time = clockInMs.toLocalDateTime(zoneId).toLocalTime()
        return if (time.isAfter(settings.standardClockIn)) PunchStatus.LATE else PunchStatus.NORMAL
    }

    fun clockOutStatus(
        clockOutMs: Long?,
        clockInMs: Long?,
        settings: AppSettings,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PunchStatus {
        if (clockOutMs == null) {
            return if (clockInMs != null) PunchStatus.MISSED_OUT else PunchStatus.INCOMPLETE
        }
        val time = clockOutMs.toLocalDateTime(zoneId).toLocalTime()
        return if (time.isBefore(settings.standardClockOut)) PunchStatus.EARLY else PunchStatus.NORMAL
    }

    fun buildRecordDetail(record: ClockRecord, settings: AppSettings, zoneId: ZoneId = ZoneId.systemDefault()): RecordDetail {
        val inStatus = clockInStatus(record.clockInTime, settings, zoneId)
        val outStatus = clockOutStatus(record.clockOutTime, record.clockInTime, settings, zoneId)
        val hours = if (record.clockInTime != null && record.clockOutTime != null) {
            hoursWorked(record.clockInTime, record.clockOutTime)
        } else {
            null
        }
        val notes = buildList {
            if (inStatus == PunchStatus.LATE) add("迟到")
            if (outStatus == PunchStatus.EARLY) add("早退")
            if (outStatus == PunchStatus.MISSED_OUT) add("漏打下班")
            if (inStatus == PunchStatus.MISSED_IN && record.clockOutTime != null) add("漏打上班")
        }
        return RecordDetail(
            record = record,
            hoursWorked = hours,
            clockInStatus = inStatus,
            clockOutStatus = outStatus,
            note = notes.joinToString(";"),
        )
    }

    fun formatTime(millis: Long?, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (millis == null) return "--"
        return millis.toLocalDateTime(zoneId).format(DateFormats.TIME_SECONDS)
    }

    fun statusLabel(status: PunchStatus): String = when (status) {
        PunchStatus.NORMAL -> "正常"
        PunchStatus.LATE -> "迟到"
        PunchStatus.EARLY -> "早退"
        PunchStatus.MISSED_IN -> "漏打上班"
        PunchStatus.MISSED_OUT -> "漏打下班"
        PunchStatus.INCOMPLETE -> "未打卡"
    }
}
