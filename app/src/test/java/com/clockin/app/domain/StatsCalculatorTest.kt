package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class StatsCalculatorTest {
    private val settings = defaultSettings()
    private val cycle = PayCycle(date(2026, 6, 9), date(2026, 7, 8))

    @Test
    fun cycleStats_countsDaysHoursAndStatuses() {
        val records = listOf(
            ClockRecord(
                shiftDate = "2026-06-10",
                clockInTime = millisAt(date(2026, 6, 10), LocalTime.of(7, 10)),
                clockOutTime = millisAt(date(2026, 6, 11), LocalTime.of(5, 0)),
            ),
            ClockRecord(
                shiftDate = "2026-06-11",
                clockInTime = millisAt(date(2026, 6, 11), LocalTime.of(6, 50)),
                clockOutTime = millisAt(date(2026, 6, 12), LocalTime.of(4, 30)),
            ),
            ClockRecord(
                shiftDate = "2026-06-12",
                clockInTime = millisAt(date(2026, 6, 12), LocalTime.of(7, 0)),
                clockOutTime = null,
            ),
        )

        val stats = StatsCalculator.cycleStats(records, settings)
        assertEquals(3, stats.clockedDays)
        assertEquals(2, stats.completeCount)
        assertEquals(1, stats.lateCount)
        assertEquals(1, stats.earlyCount)
        assertEquals(1, stats.missedOutCount)
        assertTrue(stats.totalHours > 0)
    }

    @Test
    fun targetProgress_calculatesRemainingAndDailyAverage() {
        val records = listOf(
            ClockRecord(
                shiftDate = "2026-06-10",
                clockInTime = millisAt(date(2026, 6, 10), LocalTime.of(7, 0)),
                clockOutTime = millisAt(date(2026, 6, 11), LocalTime.of(5, 0)),
            ),
        )
        val progress = StatsCalculator.targetProgress(
            records = records,
            settings = settings,
            cycle = cycle,
            today = date(2026, 6, 19),
        )

        assertEquals(1, progress.clockedDays)
        assertEquals(20, progress.remainingDays)
        assertEquals(20, progress.cycleDaysRemaining)
        assertEquals(22.0, progress.currentDailyAvg!!, 0.01)
        assertEquals(6.9, progress.requiredDailyAvg!!, 0.01)
        assertEquals(false, progress.requiredDailyUnreachable)
    }

    @Test
    fun targetProgress_whenGoalReached_requiredDailyIsNull() {
        val records = (1..21).map { day ->
            ClockRecord(
                shiftDate = "2026-06-${"%02d".format(day)}",
                clockInTime = millisAt(date(2026, 6, day), LocalTime.of(7, 0)),
                clockOutTime = millisAt(date(2026, 6, day + 1), LocalTime.of(5, 0)),
            )
        }
        val progress = StatsCalculator.targetProgress(records, settings, cycle, today = date(2026, 6, 30))
        assertTrue(progress.remainingDays <= 0)
        assertTrue(progress.remainingHours <= 0)
        assertNull(progress.requiredDailyAvg)
    }

    @Test
    fun formatRemaining_showsPlusWhenExceeded() {
        assertEquals("+2 天", StatsCalculator.formatRemainingDays(-2))
        assertEquals("+3.5h", StatsCalculator.formatRemainingHours(-3.5))
    }
}
