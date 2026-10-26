package com.clockin.app.holiday

import com.clockin.app.domain.HolidayCalendarYear
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class HolidayYearCache(
    val calendar: HolidayCalendarYear? = null,
    val lastAttemptMillis: Long = 0,
    val lastCheckedMillis: Long = 0,
)

interface HolidayCalendarStore {
    val entries: Flow<Map<Int, HolidayYearCache>>
    suspend fun save(entries: Map<Int, HolidayYearCache>)
}

data class HolidayCalendarStatus(
    val isRefreshing: Boolean = false,
    val cachedYears: List<Int> = emptyList(),
    val lastCheckedMillis: Long? = null,
    val errorMessage: String? = null,
)

data class HolidayRefreshResult(
    val checkedYears: Set<Int> = emptySet(),
    val unavailableYears: Set<Int> = emptySet(),
    val failedYears: Set<Int> = emptySet(),
)

class HolidayCalendarRepository(
    private val store: HolidayCalendarStore,
    private val fetcher: HolidayCalendarFetcher = HolidayCalendarClient(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val onCalendarsChanged: () -> Unit = {},
) {
    private val refreshMutex = Mutex()
    private val refreshing = MutableStateFlow(false)
    private val lastError = MutableStateFlow<String?>(null)

    val calendars: Flow<Map<Int, HolidayCalendarYear>> = store.entries.map { entries ->
        entries.mapNotNull { (year, cache) -> cache.calendar?.let { year to it } }.toMap()
    }.distinctUntilChanged()

    val status: Flow<HolidayCalendarStatus> = combine(store.entries, refreshing, lastError) { entries, busy, error ->
        HolidayCalendarStatus(
            isRefreshing = busy,
            cachedYears = entries.filterValues { it.calendar != null }.keys.sorted(),
            lastCheckedMillis = entries.values.map { it.lastCheckedMillis }
                .filter { it > 0 }.maxOrNull(),
            errorMessage = error ?: entries.filterValues {
                it.lastAttemptMillis > it.lastCheckedMillis
            }.keys.takeIf { it.isNotEmpty() }?.let {
                "${it.sorted().joinToString("、")} 年检查失败，继续使用本地日历，稍后自动重试。"
            },
        )
    }

    suspend fun refresh(
        requestedYears: Set<Int> = emptySet(),
        force: Boolean = false,
        today: LocalDate = LocalDate.now(),
    ): HolidayRefreshResult = refreshMutex.withLock {
        val existing = store.entries.first()
        val now = clock()
        val years = (requestedYears + existing.keys + (today.year - 1..today.year + 1))
            .filter { it in 2007..(today.year + 1) }
            .filter { force || shouldRefresh(existing[it], now) }
            .sorted()
        if (years.isEmpty()) return@withLock HolidayRefreshResult(
            failedYears = existing.filterValues { it.lastAttemptMillis > it.lastCheckedMillis }.keys,
        )

        refreshing.value = true
        val checked = mutableSetOf<Int>()
        val unavailable = mutableSetOf<Int>()
        val failed = mutableSetOf<Int>()
        try {
            for (year in years) {
                val previous = existing[year] ?: HolidayYearCache()
                val updated = try {
                    val calendar = fetcher.fetch(year)
                    if (calendar == null) unavailable += year
                    checked += year
                    HolidayYearCache(
                        calendar = calendar ?: previous.calendar,
                        lastAttemptMillis = now,
                        lastCheckedMillis = now,
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    failed += year
                    previous.copy(lastAttemptMillis = now)
                }
                // Save each year atomically, so an interrupted multi-year refresh retains completed work.
                val current = store.entries.first()
                store.save(current + (year to updated))
                if (previous.calendar != updated.calendar) onCalendarsChanged()
            }
            lastError.value = if (failed.isEmpty()) null else {
                "${failed.sorted().joinToString("、")} 年检查失败，继续使用本地日历，稍后自动重试。"
            }
            HolidayRefreshResult(checked, unavailable, failed)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            lastError.value = "节假日日历更新未完成，继续使用本地日历，稍后自动重试。"
            HolidayRefreshResult(checked, unavailable, failed + years)
        } finally {
            refreshing.value = false
        }
    }

    internal fun shouldRefresh(cache: HolidayYearCache?, now: Long): Boolean {
        if (cache == null || cache.lastAttemptMillis == 0L) return true
        val failed = cache.lastAttemptMillis > cache.lastCheckedMillis
        val last = if (failed) cache.lastAttemptMillis else cache.lastCheckedMillis
        val interval = if (failed) TimeUnit.HOURS.toMillis(1) else REFRESH_INTERVAL_MILLIS
        return now < last || now - last >= interval
    }

    companion object {
        val REFRESH_INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(12)
    }
}
