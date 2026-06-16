package com.clockin.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CycleCalculatorTest {

    @Test
    fun cycleFor_startDay9_midCycle_returns9thToNextMonth8th() {
        val cycle = CycleCalculator.cycleFor(date(2026, 6, 20), cycleStartDay = 9)
        assertEquals(date(2026, 6, 9), cycle.start)
        assertEquals(date(2026, 7, 8), cycle.end)
    }

    @Test
    fun cycleFor_startDay9_beforeStartDay_belongsToPreviousCycle() {
        val cycle = CycleCalculator.cycleFor(date(2026, 6, 5), cycleStartDay = 9)
        assertEquals(date(2026, 5, 9), cycle.start)
        assertEquals(date(2026, 6, 8), cycle.end)
    }

    @Test
    fun cycleFor_startDay9_onStartDay_includesStartDay() {
        val cycle = CycleCalculator.cycleFor(date(2026, 6, 9), cycleStartDay = 9)
        assertEquals(true, cycle.contains(date(2026, 6, 9)))
        assertEquals(true, cycle.contains(date(2026, 7, 8)))
        assertEquals(false, cycle.contains(date(2026, 7, 9)))
    }

    @Test
    fun nextAndPreviousCycle_navigateCorrectly() {
        val current = CycleCalculator.cycleFor(date(2026, 6, 20), cycleStartDay = 9)
        val next = CycleCalculator.nextCycle(current, cycleStartDay = 9)
        assertEquals(date(2026, 7, 9), next.start)
        assertEquals(date(2026, 8, 8), next.end)

        val prev = CycleCalculator.previousCycle(current, cycleStartDay = 9)
        assertEquals(date(2026, 5, 9), prev.start)
        assertEquals(date(2026, 6, 8), prev.end)
    }

    @Test
    fun cycleDaysRemaining_countsInclusiveDays() {
        val cycle = PayCycle(date(2026, 6, 9), date(2026, 7, 8))
        assertEquals(20, CycleCalculator.cycleDaysRemaining(cycle, today = date(2026, 6, 19)))
        assertEquals(1, CycleCalculator.cycleDaysRemaining(cycle, today = date(2026, 7, 8)))
        assertEquals(0, CycleCalculator.cycleDaysRemaining(cycle, today = date(2026, 7, 9)))
    }
}
