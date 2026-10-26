package com.clockin.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** Mainland China holiday and make-up workday data. Sources: docs/workday-calendar.md. */
object WorkdayCalendar {
    val supportedYears: IntRange = 2024..2026

    private val holidays = buildSet {
        addRange("2024-01-01", "2024-01-01")
        addRange("2024-02-10", "2024-02-17")
        addRange("2024-04-04", "2024-04-06")
        addRange("2024-05-01", "2024-05-05")
        addRange("2024-06-10", "2024-06-10")
        addRange("2024-09-15", "2024-09-17")
        addRange("2024-10-01", "2024-10-07")
        addRange("2025-01-01", "2025-01-01")
        addRange("2025-01-28", "2025-02-04")
        addRange("2025-04-04", "2025-04-06")
        addRange("2025-05-01", "2025-05-05")
        addRange("2025-05-31", "2025-06-02")
        addRange("2025-10-01", "2025-10-08")
        addRange("2026-01-01", "2026-01-03")
        addRange("2026-02-15", "2026-02-23")
        addRange("2026-04-04", "2026-04-06")
        addRange("2026-05-01", "2026-05-05")
        addRange("2026-06-19", "2026-06-21")
        addRange("2026-09-25", "2026-09-27")
        addRange("2026-10-01", "2026-10-07")
    }

    private val makeupWorkdays = setOf(
        "2024-02-04", "2024-02-18", "2024-04-07", "2024-04-28",
        "2024-05-11", "2024-09-14", "2024-09-29", "2024-10-12",
        "2025-01-26", "2025-02-08", "2025-04-27", "2025-09-28", "2025-10-11",
        "2026-01-04", "2026-02-14", "2026-02-28", "2026-05-09",
        "2026-09-20", "2026-10-10",
    ).mapTo(mutableSetOf()) { LocalDate.parse(it) }

    fun isWorkday(date: LocalDate, settings: AppSettings, record: ClockRecord? = null): Boolean {
        val override = record?.takeIf { it.shiftDate == date.toShiftDateString() }?.daySchedule
        return when (override ?: DaySchedule.AUTO) {
            DaySchedule.WORKDAY -> true
            DaySchedule.REST_DAY -> false
            DaySchedule.AUTO -> when {
                !settings.useWorkdayCalendar -> true
                else -> automaticWorkday(date, settings.holidayCalendars)
            }
        }
    }

    private fun automaticWorkday(date: LocalDate, calendars: Map<Int, HolidayCalendarYear>): Boolean {
        // A newer year's official notice can amend dates in the preceding December.
        val onlineOverride = calendars.entries.sortedByDescending { it.key }
            .firstNotNullOfOrNull { it.value.days[date] }
        if (onlineOverride != null) return !onlineOverride
        val hasOnlineYear = calendars.containsKey(date.year)
        return when {
            !hasOnlineYear && date in makeupWorkdays -> true
            !hasOnlineYear && date in holidays -> false
            else -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
        }
    }

    fun coverageNotice(cycle: PayCycle, settings: AppSettings = AppSettings()): String? {
        val missingYears = (cycle.start.year..cycle.end.year).filterNot {
            it in supportedYears || settings.holidayCalendars.containsKey(it)
        }
        if (missingYears.isEmpty()) return null
        return "${missingYears.joinToString("、")} 年完整节假日安排尚未获取，目前仅按周末排休；联网后会自动检查，也可在设置中立即更新。"
    }

    private fun MutableSet<LocalDate>.addRange(start: String, end: String) {
        var date = LocalDate.parse(start)
        val lastDate = LocalDate.parse(end)
        while (!date.isAfter(lastDate)) {
            add(date)
            date = date.plusDays(1)
        }
    }
}
