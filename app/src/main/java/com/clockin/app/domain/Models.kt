package com.clockin.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class TargetProgressMode {
    DAYS_AND_HOURS,
    EFFECTIVE_ATTENDANCE,
}

enum class DaySchedule { AUTO, WORKDAY, REST_DAY }

data class AttendanceTier(
    val minimumHours: Float,
    val attendanceCredit: Float,
    val absenceCredit: Float = 0f,
) {
    val isValid: Boolean
        get() = minimumHours.isFinite() && minimumHours >= 0f &&
            attendanceCredit.isFinite() && attendanceCredit >= 0f &&
            absenceCredit.isFinite() && absenceCredit >= 0f
}

val DefaultAttendanceTiers = listOf(
    AttendanceTier(minimumHours = 0f, attendanceCredit = 0f, absenceCredit = 0.5f),
    AttendanceTier(minimumHours = 6f, attendanceCredit = 0.5f),
    AttendanceTier(minimumHours = 8f, attendanceCredit = 1f),
)

data class AppSettings(
    val standardClockIn: LocalTime = LocalTime.of(7, 0),
    val standardClockOut: LocalTime = LocalTime.of(5, 0),
    val isClockOutNextDay: Boolean = true,
    val cycleStartDay: Int = 9,
    val targetDays: Int = 21,
    val targetHours: Float = 160f,
    /** 历史日历格内工时小数位数：1 或 2 */
    val calendarHoursDecimalPlaces: Int = 1,
    /** 打卡页是否显示推荐下班时间 */
    val showExpectedClockOut: Boolean = true,
    /** 打卡页是否显示当前班次已上班时长 */
    val showElapsedWorkTime: Boolean = true,
    /** 打卡页是否显示迟到、早退等状态标签 */
    val showPunchStatus: Boolean = true,
    /** 打卡页是否显示当前周期的目标进度卡片 */
    val showHomeTargetProgress: Boolean = true,
    /** 历史页是否显示当前周期的汇总卡片 */
    val showHistoryCycleSummary: Boolean = true,
    /** 打卡页目标进度的统计口径 */
    val targetProgressMode: TargetProgressMode = TargetProgressMode.EFFECTIVE_ATTENDANCE,
    /** 按最低工时升序匹配的有效出勤折算规则 */
    val attendanceTiers: List<AttendanceTier> = DefaultAttendanceTiers,
    val useWorkdayCalendar: Boolean = true,
    /** Runtime calendar cache; refreshed independently of user preferences and backups. */
    val holidayCalendars: Map<Int, HolidayCalendarYear> = emptyMap(),
)

data class ClockRecord(
    val shiftDate: String,
    val clockInTime: Long?,
    val clockOutTime: Long?,
    val isLeave: Boolean = false,
    val daySchedule: DaySchedule = DaySchedule.AUTO,
    val courseMinutes: Int = 0,
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
) {
    val isTargetMet: Boolean
        get() = remainingDays <= 0 && remainingHours <= 0
}

data class EffectiveAttendanceProgress(
    val effectiveAttendance: Double,
    val absentCount: Double,
    val leaveCount: Int,
    val cycleDaysRemaining: Int,
    val workdays: Int,
    val settledWorkdays: Int,
    val currentWorkdays: Int,
    val remainingWorkdays: Int,
    val restDays: Int,
    val clockedDays: Int,
    val punchMinutes: Double,
    val courseMinutes: Double,
    val creditedMinutes: Double,
    val fullAttendanceDays: Int,
    val halfAttendanceDays: Int,
    val shortAttendanceDays: Int,
    val fullAbsenceDays: Int,
    val incompletePunchDays: Int,
    val pendingDays: Int,
    val unmatchedTierDays: Int,
    val tierBreakdown: List<AttendanceTierSummary>,
) {
    val effectiveMinutes: Double get() = punchMinutes + courseMinutes
    val expectedMinutes: Double get() = workdays * 480.0
    val attendanceRate: Double?
        get() = if (expectedMinutes > 0) creditedMinutes / expectedMinutes * 100.0 else null
    val currentExpectedMinutes: Double get() = currentWorkdays * 480.0
    val currentAttendanceRate: Double?
        get() = if (currentExpectedMinutes > 0) creditedMinutes / currentExpectedMinutes * 100.0 else null
    val remainingMinutes: Double get() = (expectedMinutes - creditedMinutes).coerceAtLeast(0.0)
}

data class AttendanceTierSummary(
    val tier: AttendanceTier,
    val maximumHoursExclusive: Float?,
    val days: Int,
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
