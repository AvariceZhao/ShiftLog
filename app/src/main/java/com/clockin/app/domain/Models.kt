package com.clockin.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppSettings(
    val standardClockIn: LocalTime = LocalTime.of(7, 0),
    val standardClockOut: LocalTime = LocalTime.of(5, 0),
    val isClockOutNextDay: Boolean = true,
    val cycleStartDay: Int = 9,
    val targetDays: Int = 21,
    val targetHours: Float = 160f,
)

data class ClockRecord(
    val shiftDate: String,
    val clockInTime: Long?,
    val clockOutTime: Long?,
)

data class PayCycle(
    val start: LocalDate,
    val end: LocalDate,
) {
    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun label(): String {
        val fmt = DateTimeFormatter.ofPattern("M/d")
        return "${start.format(fmt)} ~ ${end.format(fmt)}"
    }

    fun fileLabel(): String = "${start}_$end"
}

enum class PunchStatus { NORMAL, LATE, EARLY, MISSED_IN, MISSED_OUT, INCOMPLETE }

data class RecordDetail(
    val record: ClockRecord,
    val hoursWorked: Double?,
    val clockInStatus: PunchStatus,
    val clockOutStatus: PunchStatus,
    val note: String,
)

data class CycleStats(
    val clockedDays: Int,
    val totalHours: Double,
    val lateCount: Int,
    val earlyCount: Int,
    /** 上下班均未打卡 */
    val absentCount: Int,
    /** 只打了上班未打下班，或有下班无上班 */
    val incompletePunchCount: Int,
    val completeCount: Int,
)

data class TargetProgress(
    val targetDays: Int,
    val targetHours: Float,
    val clockedDays: Int,
    val totalHours: Double,
    val remainingDays: Int,
    val remainingHours: Double,
    val currentDailyAvg: Double?,
    val cycleDaysRemaining: Int,
    /** 剩余工时 ÷ 目标剩余出勤天数；天数已满时不计算 */
    val requiredDailyByTargetDays: Double?,
    val requiredDailyByTargetDaysUnreachable: Boolean,
    /** 剩余工时 ÷ 周期剩余日历天数 */
    val requiredDailyByCycleDays: Double?,
    val requiredDailyByCycleDaysUnreachable: Boolean,
    /** 出勤天数已满、工时未满：只展示剩余工时，不展示目标日均 */
    val daysTargetMetHoursNotMet: Boolean,
)

object DateFormats {
    val SHIFT_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val TIME_SECONDS: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
}

fun Long.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), zoneId)

fun LocalDate.toShiftDateString(): String = format(DateFormats.SHIFT_DATE)

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, DateFormats.SHIFT_DATE)
