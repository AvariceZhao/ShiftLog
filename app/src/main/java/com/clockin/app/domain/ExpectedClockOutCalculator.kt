package com.clockin.app.domain

import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExpectedClockOutHint(
    val label: String,
    val timeText: String,
    val hoursPerDayText: String,
    val unreachable: Boolean,
)

data class ExpectedClockOut(
    val byTargetDays: ExpectedClockOutHint?,
    val byCycleDays: ExpectedClockOutHint?,
)

object ExpectedClockOutCalculator {
    private val DATE_PREFIX: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")
    private const val EFFECTIVE_ATTENDANCE_HOURS = 8.0

    fun calculate(
        clockInMs: Long?,
        clockOutMs: Long?,
        progress: TargetProgress,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ExpectedClockOut? {
        if (clockInMs == null || clockOutMs != null) return null
        if (progress.remainingHours <= 0) return null

        val byTargetDays = hintByTargetDays(clockInMs, progress, zoneId)
        val byCycleDays = hintByCycleDays(clockInMs, progress, zoneId)
        if (byTargetDays == null && byCycleDays == null) return null
        return ExpectedClockOut(byTargetDays, byCycleDays)
    }

    fun calculateForEffectiveAttendance(
        clockInMs: Long?,
        clockOutMs: Long?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ExpectedClockOut? {
        if (clockInMs == null || clockOutMs != null) return null
        return ExpectedClockOut(
            byTargetDays = buildHint(
                clockInMs = clockInMs,
                hours = EFFECTIVE_ATTENDANCE_HOURS,
                unreachable = false,
                label = "按目标出勤",
                hoursSuffix = "所需日均",
                zoneId = zoneId,
            ),
            byCycleDays = null,
        )
    }

    internal fun formatExpectedClockOut(
        clockInMs: Long,
        hoursToWork: Double,
        zoneId: ZoneId,
    ): String {
        val clockIn = clockInMs.toLocalDateTime(zoneId)
        val clockOutMs = clockInMs + kotlin.math.round(hoursToWork * 3_600_000).toLong()
        val clockOut = clockOutMs.toLocalDateTime(zoneId)
        val time = clockOut.format(DateFormats.TIME)
        return if (clockOut.toLocalDate() != clockIn.toLocalDate()) {
            "${clockOut.format(DATE_PREFIX)} $time"
        } else {
            time
        }
    }

    private fun hintByTargetDays(
        clockInMs: Long,
        progress: TargetProgress,
        zoneId: ZoneId,
    ): ExpectedClockOutHint? {
        if (progress.daysTargetMetHoursNotMet) {
            return buildHint(
                clockInMs = clockInMs,
                hours = progress.remainingHours,
                unreachable = progress.remainingHours > 24.0,
                label = "按目标出勤",
                hoursSuffix = "剩余工时",
                zoneId = zoneId,
            )
        }
        val daily = progress.requiredDailyByTargetDays ?: return null
        return buildHint(
            clockInMs = clockInMs,
            hours = daily,
            unreachable = progress.requiredDailyByTargetDaysUnreachable,
            label = "按目标出勤",
            hoursSuffix = "所需日均",
            zoneId = zoneId,
        )
    }

    private fun hintByCycleDays(
        clockInMs: Long,
        progress: TargetProgress,
        zoneId: ZoneId,
    ): ExpectedClockOutHint? {
        val daily = progress.requiredDailyByCycleDays ?: return null
        return buildHint(
            clockInMs = clockInMs,
            hours = daily,
            unreachable = progress.requiredDailyByCycleDaysUnreachable,
            label = "按周期日历",
            hoursSuffix = "所需日均",
            zoneId = zoneId,
        )
    }

    private fun buildHint(
        clockInMs: Long,
        hours: Double,
        unreachable: Boolean,
        label: String,
        hoursSuffix: String,
        zoneId: ZoneId,
    ): ExpectedClockOutHint {
        val hoursPerDayText = if (hoursSuffix == "剩余工时") {
            "${StatsCalculator.formatRemainingHours(hours)}"
        } else {
            "${"%.1f".format(hours)}h/天"
        }
        if (unreachable || hours > 24.0) {
            return ExpectedClockOutHint(
                label = label,
                timeText = "无法达成",
                hoursPerDayText = hoursPerDayText,
                unreachable = true,
            )
        }
        return ExpectedClockOutHint(
            label = label,
            timeText = formatExpectedClockOut(clockInMs, hours, zoneId),
            hoursPerDayText = hoursPerDayText,
            unreachable = false,
        )
    }
}
