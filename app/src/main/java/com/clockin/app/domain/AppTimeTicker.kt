package com.clockin.app.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/** 在 App 常驻时按日历日 / 分钟触发刷新，避免跨天、跨周期界面不更新 */
object AppTimeTicker {
    fun localDateFlow(): Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val delayMs = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L)
            delay(delayMs)
        }
    }.distinctUntilChanged()

    /** 每分钟触发，用于跨夜班次边界刷新当前班次 */
    fun minuteTickFlow(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }
}
