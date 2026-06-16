package com.clockin.app.domain

import java.time.LocalDate
import java.time.YearMonth

object CycleCalculator {
    fun cycleFor(date: LocalDate, cycleStartDay: Int): PayCycle {
        val day = cycleStartDay.coerceIn(1, 28)
        val start = if (date.dayOfMonth >= day) {
            LocalDate.of(date.year, date.month, day)
        } else {
            val prev = date.minusMonths(1)
            LocalDate.of(prev.year, prev.month, day)
        }
        val endMonth = YearMonth.from(start).plusMonths(1)
        val end = if (day == 1) {
            YearMonth.from(start).atEndOfMonth()
        } else {
            LocalDate.of(endMonth.year, endMonth.month, day - 1)
        }
        return PayCycle(start, end)
    }

    fun currentCycle(cycleStartDay: Int, today: LocalDate = LocalDate.now()): PayCycle =
        cycleFor(today, cycleStartDay)

    fun previousCycle(cycle: PayCycle, cycleStartDay: Int): PayCycle {
        val prevAnchor = cycle.start.minusDays(1)
        return cycleFor(prevAnchor, cycleStartDay)
    }

    fun nextCycle(cycle: PayCycle, cycleStartDay: Int): PayCycle {
        val nextAnchor = cycle.end.plusDays(1)
        return cycleFor(nextAnchor, cycleStartDay)
    }

    fun cycleDaysRemaining(cycle: PayCycle, today: LocalDate = LocalDate.now()): Int {
        if (today.isAfter(cycle.end)) return 0
        val startCount = if (today.isBefore(cycle.start)) cycle.start else today
        return java.time.temporal.ChronoUnit.DAYS.between(startCount, cycle.end).toInt() + 1
    }
}
