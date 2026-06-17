package com.clockin.app.domain

import java.time.LocalDate
import java.time.LocalDateTime

enum class CycleDayStatus {
    /** 周期内未到评估时间 */
    PENDING,
    /** 正常出勤 */
    NORMAL,
    /** 迟到、早退或缺卡 */
    ISSUE,
    /** 旷工（应出勤但未打卡） */
    ABSENT,
    /** 周期外占位 */
    OUT_OF_CYCLE,
}

object CycleDayStatusCalculator {
    fun statusForDate(
        date: LocalDate,
        cycle: PayCycle,
        record: ClockRecord?,
        settings: AppSettings,
        today: LocalDate = LocalDate.now(),
        now: LocalDateTime = LocalDateTime.now(),
    ): CycleDayStatus {
        if (!cycle.contains(date)) return CycleDayStatus.OUT_OF_CYCLE
        if (date.isAfter(today)) return CycleDayStatus.PENDING
        if (date == today && now.isBefore(LocalDateTime.of(date, settings.standardClockIn))) {
            return CycleDayStatus.PENDING
        }

        val shiftDate = date.toShiftDateString()
        val existing = record?.takeIf { it.shiftDate == shiftDate }
        if (existing == null || (existing.clockInTime == null && existing.clockOutTime == null)) {
            return CycleDayStatus.ABSENT
        }
        if (existing.clockInTime == null || existing.clockOutTime == null) {
            if (date.isBefore(today) || isShiftEnded(date, now, settings)) {
                return CycleDayStatus.ISSUE
            }
            return CycleDayStatus.PENDING
        }

        val detail = ShiftCalculator.buildRecordDetail(existing, settings)
        return if (
            detail.clockInStatus == PunchStatus.LATE ||
            detail.clockOutStatus == PunchStatus.EARLY ||
            detail.clockOutStatus == PunchStatus.MISSED_OUT ||
            detail.clockInStatus == PunchStatus.MISSED_IN
        ) {
            CycleDayStatus.ISSUE
        } else {
            CycleDayStatus.NORMAL
        }
    }

    private fun isShiftEnded(shiftDate: LocalDate, now: LocalDateTime, settings: AppSettings): Boolean {
        val endDate = if (settings.isClockOutNextDay) shiftDate.plusDays(1) else shiftDate
        val end = LocalDateTime.of(endDate, settings.standardClockOut)
        return !now.isBefore(end)
    }
}
