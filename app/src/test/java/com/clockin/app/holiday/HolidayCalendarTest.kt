package com.clockin.app.holiday

import com.clockin.app.data.DataStoreHolidayCalendarStore
import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.DaySchedule
import com.clockin.app.domain.HolidayCalendarYear
import com.clockin.app.domain.PayCycle
import com.clockin.app.domain.StatsCalculator
import com.clockin.app.domain.WorkdayCalendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

internal fun sampleCalendar(year: Int): HolidayCalendarYear = HolidayCalendarYear(
    year,
    listOf("01-01", "02-01", "04-05", "05-01", "10-01", "10-02")
        .associate { LocalDate.parse("$year-$it") to true } +
        (LocalDate.parse("$year-10-10") to false),
    listOf("https://www.gov.cn/zhengce/example-$year.htm"),
)

private class MemoryCalendarStore : HolidayCalendarStore {
    var failWrites = false
    override val entries = MutableStateFlow<Map<Int, HolidayYearCache>>(emptyMap())
    override suspend fun save(entries: Map<Int, HolidayYearCache>) {
        if (failWrites) throw IOException("cache unavailable")
        this.entries.value = entries
    }
}

class HolidayCalendarTest {
    @Test
    fun annualParser_rejectsUnpublishedAndPartialSchedules() {
        assertNull(HolidayCalendarJson.parse("""{"year":2027,"papers":[],"days":[]}""", 2027))
        val partial = sampleCalendar(2027).copy(days = mapOf(LocalDate.parse("2027-01-01") to true))
        assertNull(HolidayCalendarJson.parse(HolidayCalendarJson.encode(partial).toString(), 2027))
        assertEquals(sampleCalendar(2026), HolidayCalendarJson.parse(
            HolidayCalendarJson.encode(sampleCalendar(2026)).toString(), 2026,
        ))
    }

    @Test
    fun annualParser_rejectsWrongYearMalformedDateAndNonBooleanType() {
        val valid = HolidayCalendarJson.encode(sampleCalendar(2026)).toString()
        assertTrue(runCatching { HolidayCalendarJson.parse(valid, 2027) }.isFailure)
        val malformed = JSONObject(valid).apply {
            getJSONArray("days").getJSONObject(0).put("date", "2026-02-30")
        }
        assertTrue(runCatching { HolidayCalendarJson.parse(malformed.toString(), 2026) }.isFailure)
        val invalidType = JSONObject(valid).apply {
            getJSONArray("days").getJSONObject(0).put("isOffDay", "false")
        }
        assertTrue(runCatching { HolidayCalendarJson.parse(invalidType.toString(), 2026) }.isFailure)
    }

    @Test
    fun cache_roundTripsOfflineDataAndSkipsOnlyCorruptYears() {
        val entries = mapOf(
            2026 to HolidayYearCache(sampleCalendar(2026), 5000, 5000),
            2027 to HolidayYearCache(null, 5000, 5000),
        )
        val raw = DataStoreHolidayCalendarStore.encode(entries)
        assertEquals(entries, DataStoreHolidayCalendarStore.decode(raw))
        val damaged = JSONObject(raw).put("2025", "broken")
        assertEquals(entries, DataStoreHolidayCalendarStore.decode(damaged.toString()))
    }

    @Test
    fun refresh_rechecksCachedYearsAfterTwelveHoursAndReplacesRemovedDates() = runBlocking {
        val store = MemoryCalendarStore()
        var now = TimeUnit.DAYS.toMillis(100)
        var current = sampleCalendar(2026)
        val calls = mutableListOf<Int>()
        val repository = HolidayCalendarRepository(store, HolidayCalendarFetcher { year ->
            calls += year
            if (year == 2026) current else null
        }, clock = { now })
        val today = LocalDate.parse("2026-10-26")
        repository.refresh(today = today)
        val initialCalls = calls.size
        repository.refresh(today = today)
        assertEquals(initialCalls, calls.size)

        val removedHoliday = LocalDate.parse("2026-10-01")
        val addedHoliday = LocalDate.parse("2026-10-26")
        current = current.copy(days = current.days - removedHoliday + (addedHoliday to true))
        now += HolidayCalendarRepository.REFRESH_INTERVAL_MILLIS
        repository.refresh(today = today)
        assertEquals(initialCalls * 2, calls.size)
        val settings = AppSettings(holidayCalendars = repository.calendars.first())
        assertTrue(WorkdayCalendar.isWorkday(removedHoliday, settings))
        assertFalse(WorkdayCalendar.isWorkday(addedHoliday, settings))
    }

    @Test
    fun refresh_forceBypassesFreshCacheAndNewDataChangesStatsButNotManualOverrides() = runBlocking {
        val store = MemoryCalendarStore()
        var current = sampleCalendar(2026)
        val repository = HolidayCalendarRepository(store, HolidayCalendarFetcher { year ->
            if (year == 2026) current else null
        })
        val day = LocalDate.parse("2026-10-26")
        val start = day.atTime(7, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val record = ClockRecord(day.toString(), start, start + 8 * 3_600_000L)
        repository.refresh(today = day)
        val before = AppSettings(holidayCalendars = repository.calendars.first())
        val cycle = PayCycle(day, day)
        assertEquals(8.0, StatsCalculator.cycleStats(listOf(record), before, cycle, day.plusDays(2)).totalHours, 0.001)

        current = current.copy(days = current.days + (day to true))
        repository.refresh(force = true, today = day)
        val after = before.copy(holidayCalendars = repository.calendars.first())
        assertEquals(0.0, StatsCalculator.cycleStats(listOf(record), after, cycle, day.plusDays(2)).totalHours, 0.001)
        val manual = record.copy(daySchedule = DaySchedule.WORKDAY)
        assertEquals(8.0, StatsCalculator.cycleStats(listOf(manual), after, cycle, day.plusDays(2)).totalHours, 0.001)
    }

    @Test
    fun refresh_networkFailureKeepsCacheAndThrottlesRetryForOneHour() = runBlocking {
        val store = MemoryCalendarStore()
        var now = TimeUnit.DAYS.toMillis(100)
        var fail = false
        var calls = 0
        val repository = HolidayCalendarRepository(store, HolidayCalendarFetcher { year ->
            calls++
            if (fail) throw IOException("offline")
            if (year == 2026) sampleCalendar(year) else null
        }, clock = { now })
        val today = LocalDate.parse("2026-10-26")
        repository.refresh(today = today)
        val cached = repository.calendars.first()
        fail = true
        now += HolidayCalendarRepository.REFRESH_INTERVAL_MILLIS
        val failure = repository.refresh(today = today)
        assertTrue(2026 in failure.failedYears)
        assertEquals(cached, repository.calendars.first())
        val failedCalls = calls
        now += TimeUnit.MINUTES.toMillis(59)
        repository.refresh(today = today)
        assertEquals(failedCalls, calls)
        now += TimeUnit.MINUTES.toMillis(1)
        repository.refresh(today = today)
        assertTrue(calls > failedCalls)
    }

    @Test
    fun refresh_emptyResponseNeverErasesPreviouslyDownloadedYear() = runBlocking {
        val store = MemoryCalendarStore()
        var empty = false
        val repository = HolidayCalendarRepository(store, HolidayCalendarFetcher { year ->
            if (empty || year != 2026) null else sampleCalendar(year)
        })
        val today = LocalDate.parse("2026-10-26")
        repository.refresh(today = today)
        empty = true
        val result = repository.refresh(force = true, today = today)
        assertTrue(2026 in result.unavailableYears)
        assertEquals(sampleCalendar(2026), repository.calendars.first()[2026])
        assertFalse(repository.calendars.first().containsKey(2027))
    }

    @Test
    fun nextYearNotice_canOverridePrecedingDecemberAndClearCoverageWarning() {
        val date = LocalDate.parse("2026-12-31")
        val calendars = mapOf(
            2026 to sampleCalendar(2026).let { it.copy(days = it.days + (date to false)) },
            2027 to sampleCalendar(2027).let { it.copy(days = it.days + (date to true)) },
        )
        val settings = AppSettings(holidayCalendars = calendars)
        assertFalse(WorkdayCalendar.isWorkday(date, settings))
        assertNull(WorkdayCalendar.coverageNotice(PayCycle(date, date.plusDays(10)), settings))
    }

    @Test
    fun refresh_cancellationDoesNotBecomeNetworkFailureOrOverwriteCache() = runBlocking {
        val store = MemoryCalendarStore()
        val repository = HolidayCalendarRepository(store, HolidayCalendarFetcher { throw CancellationException() })
        val result = runCatching { repository.refresh(today = LocalDate.parse("2026-10-26")) }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertTrue(store.entries.value.isEmpty())
        assertFalse(repository.status.first().isRefreshing)
    }

    @Test
    fun refresh_concurrentStartupAndBackgroundChecksShareFreshnessGate() = runBlocking {
        val store = MemoryCalendarStore()
        val calls = mutableListOf<Int>()
        val repository = HolidayCalendarRepository(store, HolidayCalendarFetcher { year ->
            calls += year
            yield()
            if (year == 2026) sampleCalendar(year) else null
        })
        val first = async { repository.refresh(today = LocalDate.parse("2026-10-26")) }
        val second = async { repository.refresh(today = LocalDate.parse("2026-10-26")) }
        first.await()
        second.await()
        assertEquals(listOf(2025, 2026, 2027), calls)
    }

    @Test
    fun refresh_cacheWriteFailureKeepsOldCalendarAndDoesNotCrashCaller() = runBlocking {
        val store = MemoryCalendarStore()
        var current = sampleCalendar(2026)
        val repository = HolidayCalendarRepository(store, HolidayCalendarFetcher { year ->
            if (year == 2026) current else null
        })
        val today = LocalDate.parse("2026-10-26")
        repository.refresh(today = today)
        store.failWrites = true
        current = current.copy(days = current.days + (today to true))
        val failed = repository.refresh(force = true, today = today)
        assertTrue(2026 in failed.failedYears)
        assertEquals(sampleCalendar(2026), repository.calendars.first()[2026])
        assertNotNull(repository.status.first().errorMessage)
        assertFalse(repository.status.first().isRefreshing)
    }

    @Test
    fun client_usesAlternateHostAfterNetworkFailure() = runBlocking {
        val hosts = mutableListOf<String>()
        val json = HolidayCalendarJson.encode(sampleCalendar(2026)).toString()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            hosts += chain.request().url.host
            if (hosts.size == 1) throw IOException("primary unavailable")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(json.toResponseBody()).build()
        }.build()
        assertEquals(sampleCalendar(2026), HolidayCalendarClient(client).fetch(2026))
        assertEquals(listOf("raw.githubusercontent.com", "cdn.jsdelivr.net"), hosts)
    }

    @Test
    fun client_rejectsOversizedResponseAndTriesAlternateHost() = runBlocking {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            val body = if (calls == 1) "x".repeat(300_000) else {
                HolidayCalendarJson.encode(sampleCalendar(2026)).toString()
            }
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(body.toResponseBody()).build()
        }.build()
        assertEquals(sampleCalendar(2026), HolidayCalendarClient(client).fetch(2026))
        assertEquals(2, calls)
    }
}
