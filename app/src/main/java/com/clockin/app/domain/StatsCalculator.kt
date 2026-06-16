package com.clockin.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object StatsCalculator {
    fun cycleStats(
        records: List<ClockRecord>,
        settings: AppSettings,
        cycle: PayCycle,
        today: LocalDate = LocalDate.now(),
        now: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CycleStats {
        val recordByDate = records.associateBy { it.shiftDate }
        var clockedDays = 0
        var totalHours = 0.0
        var lateCount = 0
        var earlyCount = 0
        var missedInCount = 0
        var missedOutCount = 0
        var completeCount = 0

        records.forEach { record ->
            val shiftDate = LocalDate.parse(record.shiftDate, DateFormats.SHIFT_DATE)
            val detail = ShiftCalculator.buildRecordDetail(record, settings, zoneId)
            if (record.clockInTime != null) {
                clockedDays++
            }
            if (detail.hoursWorked != null) {
                totalHours += detail.hoursWorked
                completeCount++
            }
            if (detail.clockInStatus == PunchStatus.LATE) lateCount++
            if (detail.clockOutStatus == PunchStatus.EARLY) earlyCount++
            if (detail.clockInStatus == PunchStatus.MISSED_IN &&
                shouldEvaluateMissedIn(shiftDate, today, now, settings)
            ) {
                missedInCount++
            }
            if (detail.clockOutStatus == PunchStatus.MISSED_OUT &&
                shouldEvaluateMissedOut(shiftDate, now, settings)
            ) {
                missedOutCount++
            }
        }

        var date = cycle.start
        while (!date.isAfter(cycle.end) && !date.isAfter(today)) {
            val key = date.format(DateFormats.SHIFT_DATE)
            if (!recordByDate.containsKey(key) && shouldEvaluateMissedIn(date, today, now, settings)) {
                missedInCount++
            }
            date = date.plusDays(1)
        }

        return CycleStats(
            clockedDays = clockedDays,
            totalHours = totalHours,
            lateCount = lateCount,
            earlyCount = earlyCount,
            missedInCount = missedInCount,
            missedOutCount = missedOutCount,
            completeCount = completeCount,
        )
    }

    private fun shiftStartDateTime(shiftDate: LocalDate, settings: AppSettings): LocalDateTime =
        LocalDateTime.of(shiftDate, settings.standardClockIn)

    private fun shiftEndDateTime(shiftDate: LocalDate, settings: AppSettings): LocalDateTime {
        val endDate = if (settings.isClockOutNextDay) shiftDate.plusDays(1) else shiftDate
        return LocalDateTime.of(endDate, settings.standardClockOut)
    }

    private fun shouldEvaluateMissedIn(
        shiftDate: LocalDate,
        today: LocalDate,
        now: LocalDateTime,
        settings: AppSettings,
    ): Boolean {
        if (shiftDate.isAfter(today)) return false
        if (shiftDate.isBefore(today)) return true
        return !now.isBefore(shiftStartDateTime(shiftDate, settings))
    }

    private fun shouldEvaluateMissedOut(
        shiftDate: LocalDate,
        now: LocalDateTime,
        settings: AppSettings,
    ): Boolean = !now.isBefore(shiftEndDateTime(shiftDate, settings))

    fun targetProgress(
        records: List<ClockRecord>,
        settings: AppSettings,
        cycle: PayCycle,
        today: LocalDate = LocalDate.now(),
    ): TargetProgress {
        val stats = cycleStats(records, settings, cycle, today)
        val remainingDays = settings.targetDays - stats.clockedDays
        val remainingHours = settings.targetHours - stats.totalHours
        val cycleDaysRemaining = CycleCalculator.cycleDaysRemaining(cycle, today)
        val currentDailyAvg = if (stats.clockedDays > 0) {
            stats.totalHours / stats.clockedDays
        } else {
            null
        }
        val daysTargetMetHoursNotMet = remainingDays <= 0 && remainingHours > 0

        val (requiredDailyByTargetDays, targetUnreachable) = if (remainingDays > 0 && remainingHours > 0) {
            val avg = remainingHours / remainingDays
            avg to (avg > 24.0)
        } else {
            null to false
        }

        val (requiredDailyByCycleDays, cycleUnreachable) = if (
            !today.isBefore(cycle.start) &&
            cycleDaysRemaining > 0 &&
            remainingHours > 0
        ) {
            val avg = remainingHours / cycleDaysRemaining
            avg to (avg > 24.0)
        } else {
            null to false
        }

        return TargetProgress(
            targetDays = settings.targetDays,
            targetHours = settings.targetHours,
            clockedDays = stats.clockedDays,
            totalHours = stats.totalHours,
            remainingDays = remainingDays,
            remainingHours = remainingHours,
            currentDailyAvg = currentDailyAvg,
            cycleDaysRemaining = cycleDaysRemaining,
            requiredDailyByTargetDays = requiredDailyByTargetDays,
            requiredDailyByTargetDaysUnreachable = targetUnreachable,
            requiredDailyByCycleDays = requiredDailyByCycleDays,
            requiredDailyByCycleDaysUnreachable = cycleUnreachable,
            daysTargetMetHoursNotMet = daysTargetMetHoursNotMet,
        )
    }

    fun formatDailyAvg(hours: Double?, unreachable: Boolean): String = when {
        hours == null -> "--"
        unreachable -> "${"%.1f".format(hours)}h · 无法达成"
        else -> "${"%.1f".format(hours)}h"
    }

    fun formatRemainingDays(value: Int): String {
        return if (value < 0) "+${-value} 天" else "$value 天"
    }

    fun formatRemainingHours(value: Double): String {
        return if (value < 0) "+${"%.1f".format(-value)}h" else "${"%.1f".format(value)}h"
    }
}
