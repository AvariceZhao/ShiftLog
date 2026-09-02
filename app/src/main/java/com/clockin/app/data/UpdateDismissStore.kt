package com.clockin.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_prefs")

class UpdateDismissStore(private val context: Context) {
    private val dismissedTagKey = stringPreferencesKey("dismissed_release_tag")

    suspend fun getDismissedTag(): String? =
        context.updateDataStore.data.first()[dismissedTagKey]

    suspend fun setDismissedTag(tagName: String) {
        context.updateDataStore.edit { prefs ->
            prefs[dismissedTagKey] = tagName
        }
    }
}
