package com.clockin.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clockin.app.domain.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val clockInKey = stringPreferencesKey("standard_clock_in")
    private val clockOutKey = stringPreferencesKey("standard_clock_out")
    private val clockOutNextDayKey = booleanPreferencesKey("clock_out_next_day")
    private val cycleStartDayKey = intPreferencesKey("cycle_start_day")
    private val targetDaysKey = intPreferencesKey("target_days")
    private val targetHoursKey = floatPreferencesKey("target_hours")

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            standardClockIn = prefs[clockInKey]?.let { LocalTime.parse(it, timeFormatter) }
                ?: LocalTime.of(7, 0),
            standardClockOut = prefs[clockOutKey]?.let { LocalTime.parse(it, timeFormatter) }
                ?: LocalTime.of(5, 0),
            isClockOutNextDay = prefs[clockOutNextDayKey] ?: true,
            cycleStartDay = prefs[cycleStartDayKey] ?: 9,
            targetDays = prefs[targetDaysKey] ?: 21,
            targetHours = prefs[targetHoursKey] ?: 160f,
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[clockInKey] = settings.standardClockIn.format(timeFormatter)
            prefs[clockOutKey] = settings.standardClockOut.format(timeFormatter)
            prefs[clockOutNextDayKey] = settings.isClockOutNextDay
            prefs[cycleStartDayKey] = settings.cycleStartDay.coerceIn(1, 28)
            prefs[targetDaysKey] = settings.targetDays.coerceAtLeast(1)
            prefs[targetHoursKey] = settings.targetHours.coerceAtLeast(0f)
        }
    }
}
