package com.clockin.app.domain

import java.time.LocalDate

object StatsCalculator {
    fun cycleStats(records: List<ClockRecord>, settings: AppSettings): CycleStats {
        var clockedDays = 0
        var totalHours = 0.0
        var lateCount = 0
        var earlyCount = 0
        var missedInCount = 0
        var missedOutCount = 0
        var completeCount = 0

        records.forEach { record ->
            val detail = ShiftCalculator.buildRecordDetail(record, settings)
            if (record.clockInTime != null) {
                clockedDays++
            }
            if (detail.hoursWorked != null) {
                totalHours += detail.hoursWorked
                completeCount++
            }
            if (detail.clockInStatus == PunchStatus.LATE) lateCount++
            if (detail.clockOutStatus == PunchStatus.EARLY) earlyCount++
            if (detail.clockInStatus == PunchStatus.MISSED_IN) missedInCount++
            if (detail.clockOutStatus == PunchStatus.MISSED_OUT) missedOutCount++
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

    fun targetProgress(
        records: List<ClockRecord>,
        settings: AppSettings,
        cycle: PayCycle,
        today: LocalDate = LocalDate.now(),
    ): TargetProgress {
        val stats = cycleStats(records, settings)
        val remainingDays = settings.targetDays - stats.clockedDays
        val remainingHours = settings.targetHours - stats.totalHours
        val cycleDaysRemaining = CycleCalculator.cycleDaysRemaining(cycle, today)
        val currentDailyAvg = if (stats.clockedDays > 0) {
            stats.totalHours / stats.clockedDays
        } else {
            null
        }
        val requiredDailyAvg = if (cycleDaysRemaining > 0 && remainingHours > 0) {
            remainingHours / cycleDaysRemaining
        } else {
            null
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
            requiredDailyAvg = requiredDailyAvg,
            requiredDailyUnreachable = requiredDailyAvg != null && requiredDailyAvg > 24.0,
        )
    }

    fun formatRemainingDays(value: Int): String {
        return if (value < 0) "+${-value} 天" else "$value 天"
    }

    fun formatRemainingHours(value: Double): String {
        return if (value < 0) "+${"%.1f".format(-value)}h" else "${"%.1f".format(value)}h"
    }
}
