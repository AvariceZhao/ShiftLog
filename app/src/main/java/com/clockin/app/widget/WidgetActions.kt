package com.clockin.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetRefresh {
    suspend fun updateAll(context: Context, appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID) {
        val appContext = context.applicationContext
        withContext(Dispatchers.Main.immediate) {
            bumpRefreshTick(appContext, appWidgetId)
            val widget = CompactPanelWidget()
            widget.updateAll(appContext)
            updateViaAppWidgetIds(appContext, widget, appWidgetId)
            broadcastUpdate(appContext)
        }
    }

    private suspend fun bumpRefreshTick(context: Context, appWidgetId: Int) {
        val glanceIds = resolveGlanceIds(context, appWidgetId)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
                prefs[WidgetKeys.REFRESH_TICK] = System.currentTimeMillis()
            }
        }
    }

    private suspend fun resolveGlanceIds(
        context: Context,
        appWidgetId: Int,
    ): List<androidx.glance.GlanceId> {
        val manager = GlanceAppWidgetManager(context)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            return runCatching { listOf(manager.getGlanceIdBy(appWidgetId)) }.getOrDefault(emptyList())
        }
        return manager.getGlanceIds(CompactPanelWidget::class.java)
    }

    private suspend fun updateViaAppWidgetIds(
        context: Context,
        widget: GlanceAppWidget,
        targetAppWidgetId: Int,
    ) {
        val receiver = ComponentName(context, CompactPanelWidgetReceiver::class.java)
        val appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(receiver)
        if (appWidgetIds.isEmpty()) return
        val glanceManager = GlanceAppWidgetManager(context)
        appWidgetIds
            .filter { targetAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || it == targetAppWidgetId }
            .forEach { id ->
                runCatching {
                    val glanceId = glanceManager.getGlanceIdBy(id)
                    widget.update(context, glanceId)
                }
            }
    }

    private fun broadcastUpdate(context: Context) {
        val receiver = ComponentName(context, CompactPanelWidgetReceiver::class.java)
        val appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(receiver)
        if (appWidgetIds.isEmpty()) return
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            component = receiver
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.sendBroadcast(intent)
    }
}
