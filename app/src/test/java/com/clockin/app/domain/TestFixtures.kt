package com.clockin.app.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal val TestZone: ZoneId = ZoneId.of("Asia/Shanghai")

internal fun defaultSettings() = AppSettings(
    standardClockIn = LocalTime.of(7, 0),
    standardClockOut = LocalTime.of(5, 0),
    isClockOutNextDay = true,
    cycleStartDay = 9,
    targetDays = 21,
    targetHours = 160f,
)

internal fun millisAt(date: LocalDate, time: LocalTime, zone: ZoneId = TestZone): Long =
    LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()

internal fun date(y: Int, m: Int, d: Int): LocalDate = LocalDate.of(y, m, d)
