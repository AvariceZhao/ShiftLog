package com.clockin.app.holiday

import com.clockin.app.domain.HolidayCalendarYear
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

fun interface HolidayCalendarFetcher {
    suspend fun fetch(year: Int): HolidayCalendarYear?
}

class HolidayCalendarClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) : HolidayCalendarFetcher {
    override suspend fun fetch(year: Int): HolidayCalendarYear? = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/$year.json",
            "https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/$year.json",
        )
        var unavailable = false
        for (url in urls) {
            try {
                val request = Request.Builder().url(url)
                    .header("User-Agent", "ShiftLog-Android")
                    .header("Cache-Control", "no-cache")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 404) {
                        unavailable = true
                    } else {
                        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                        require((response.body?.contentLength() ?: 0) <= MAX_BYTES) { "节假日数据过大" }
                        val source = response.body?.source() ?: throw IOException("节假日响应为空")
                        require(!source.request(MAX_BYTES + 1)) { "节假日数据过大" }
                        val calendar = HolidayCalendarJson.parse(source.readUtf8(), year)
                        if (calendar != null) return@withContext calendar
                        unavailable = true
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Try the same dataset through its alternate host before keeping the old cache.
            }
        }
        if (unavailable) return@withContext null
        throw IOException("暂时无法连接节假日数据源")
    }

    private companion object {
        const val MAX_BYTES = 256 * 1024L
    }
}
