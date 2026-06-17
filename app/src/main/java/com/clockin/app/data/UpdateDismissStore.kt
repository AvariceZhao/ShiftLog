package com.clockin.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_prefs")

class UpdateDismissStore(private val context: Context) {
    private val dismissedTagKey = stringPreferencesKey("dismissed_release_tag")
    private val lastAutoCheckKey = longPreferencesKey("last_auto_check_millis")

    suspend fun getDismissedTag(): String? =
        context.updateDataStore.data.first()[dismissedTagKey]

    suspend fun setDismissedTag(tagName: String) {
        context.updateDataStore.edit { prefs ->
            prefs[dismissedTagKey] = tagName
        }
    }

    suspend fun shouldRunAutoCheck(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val last = context.updateDataStore.data.first()[lastAutoCheckKey] ?: 0L
        return nowMillis - last >= TimeUnit.HOURS.toMillis(24)
    }

    suspend fun markAutoCheckRan(nowMillis: Long = System.currentTimeMillis()) {
        context.updateDataStore.edit { prefs ->
            prefs[lastAutoCheckKey] = nowMillis
        }
    }
}
