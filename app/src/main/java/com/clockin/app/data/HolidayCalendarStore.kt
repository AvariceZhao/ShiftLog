package com.clockin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clockin.app.holiday.HolidayCalendarJson
import com.clockin.app.holiday.HolidayCalendarStore
import com.clockin.app.holiday.HolidayYearCache
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import org.json.JSONObject
import java.io.IOException

private val Context.holidayCalendarDataStore by preferencesDataStore(name = "holiday_calendar")

class DataStoreHolidayCalendarStore(private val context: Context) : HolidayCalendarStore {
    private val cacheKey = stringPreferencesKey("year_cache")

    override val entries = context.holidayCalendarDataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs ->
        decode(prefs[cacheKey])
    }

    override suspend fun save(entries: Map<Int, HolidayYearCache>) {
        context.holidayCalendarDataStore.edit { prefs ->
            prefs[cacheKey] = encode(entries)
        }
    }

    internal companion object {
        fun decode(raw: String?): Map<Int, HolidayYearCache> {
            if (raw.isNullOrBlank()) return emptyMap()
            val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
            return root.keys().asSequence().mapNotNull { key ->
                runCatching {
                    val year = key.toInt()
                    val item = root.getJSONObject(key)
                    year to HolidayYearCache(
                        calendar = item.optJSONObject("calendar")?.let {
                            HolidayCalendarJson.parse(it.toString(), year)
                        },
                        lastAttemptMillis = item.optLong("lastAttemptMillis", 0),
                        lastCheckedMillis = item.optLong("lastCheckedMillis", 0),
                    )
                }.getOrNull()
            }.toMap()
        }

        fun encode(entries: Map<Int, HolidayYearCache>): String = JSONObject().apply {
            entries.forEach { (year, entry) ->
                put(year.toString(), JSONObject().apply {
                    put("calendar", entry.calendar?.let(HolidayCalendarJson::encode) ?: JSONObject.NULL)
                    put("lastAttemptMillis", entry.lastAttemptMillis)
                    put("lastCheckedMillis", entry.lastCheckedMillis)
                })
            }
        }.toString()
    }
}
