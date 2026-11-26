package com.clockin.app.domain

import java.time.LocalDate
import java.time.LocalDateTime

/** Attendance duration and attendance credits are deliberately independent measures. */
object AttendanceCalculator {
    fun calculate(
        records: List<ClockRecord>,
        settings: AppSettings,
        cycle: PayCycle,
        today: LocalDate,
        now: LocalDateTime,
    ): EffectiveAttendanceProgress {
        val byDate = records.associateBy { it.shiftDate }
        val tiers = settings.attendanceTiers.filter { it.isValid }
            .distinctBy { it.minimumHours }.sortedBy { it.minimumHours }
        val tierCounts = IntArray(tiers.size)
        var workdays = 0
        var settledWorkdays = 0
        var currentWorkdays = 0
        var remainingWorkdays = 0
        var restDays = 0
        var clockedDays = 0
        var punchMinutes = 0.0
        var courseMinutes = 0.0
        var creditedMinutes = 0.0
        var attendance = 0.0
        var absence = 0.0
        var leave = 0
        var full = 0
        var half = 0
        var short = 0
        var fullAbsent = 0
        var incomplete = 0
        var pending = 0
        var unmatched = 0
        var date = cycle.start
        while (!date.isAfter(cycle.end)) {
            val record = byDate[date.toShiftDateString()]
            if (!WorkdayCalendar.isWorkday(date, settings, record)) {
                restDays++
                date = date.plusDays(1)
                continue
            }
            workdays++
            val started = !date.isAfter(today) && !now.isBefore(date.atTime(settings.standardClockIn))
            val ended = !date.isAfter(today) && !now.isBefore(ShiftCalculator.expectedShiftEnd(date, settings))
            if (ended) settledWorkdays++
            if (ended) currentWorkdays++
            if (!ended) remainingWorkdays++
            if (!started) {
                pending++
                date = date.plusDays(1)
                continue
            }
            if (record?.isLeave == true) {
                leave++
                date = date.plusDays(1)
                continue
            }
            val hasIn = record?.clockInTime != null
            val hasOut = record?.clockOutTime != null
            val hasPunch = hasIn || hasOut
            val complete = hasIn && hasOut
            val course = record?.courseMinutes?.coerceAtLeast(0)?.toDouble() ?: 0.0
            if (hasPunch) clockedDays++
            // In-progress shifts must not receive a short-day penalty before checkout.
            if ((!hasPunch && course == 0.0 || hasPunch && !complete) && !ended) {
                pending++
                date = date.plusDays(1)
                continue
            }
            // Completed early shifts and course-only days join both sides of the current rate.
            if (!ended) currentWorkdays++
            if (!hasPunch && course == 0.0) {
                fullAbsent++
                absence += 1.0
            } else {
                if (hasPunch && !complete) incomplete++
                val punch = if (complete) {
                    ShiftCalculator.hoursWorked(record!!.clockInTime!!, record.clockOutTime!!) * 60.0
                } else 0.0
                val effective = punch + course
                punchMinutes += punch
                courseMinutes += course
                creditedMinutes += effective.coerceAtMost(480.0)
                when {
                    effective >= 480.0 -> full++
                    effective >= 360.0 -> half++
                    else -> short++
                }
                val index = tiers.indexOfLast { effective / 60.0 >= it.minimumHours }
                if (index >= 0) {
                    tierCounts[index]++
                    attendance += tiers[index].attendanceCredit.toDouble()
                    absence += tiers[index].absenceCredit.toDouble()
                } else unmatched++
            }
            date = date.plusDays(1)
        }
        return EffectiveAttendanceProgress(
            effectiveAttendance = attendance,
            absentCount = absence,
            leaveCount = leave,
            cycleDaysRemaining = CycleCalculator.cycleDaysRemaining(cycle, today),
            workdays = workdays,
            settledWorkdays = settledWorkdays,
            currentWorkdays = currentWorkdays,
            remainingWorkdays = remainingWorkdays,
            restDays = restDays,
            clockedDays = clockedDays,
            punchMinutes = punchMinutes,
            courseMinutes = courseMinutes,
            creditedMinutes = creditedMinutes,
            fullAttendanceDays = full,
            halfAttendanceDays = half,
            shortAttendanceDays = short,
            fullAbsenceDays = fullAbsent,
            incompletePunchDays = incomplete,
            pendingDays = pending,
            unmatchedTierDays = unmatched,
            tierBreakdown = tiers.mapIndexed { index, tier ->
                AttendanceTierSummary(tier, tiers.getOrNull(index + 1)?.minimumHours, tierCounts[index])
            },
        )
    }
}
