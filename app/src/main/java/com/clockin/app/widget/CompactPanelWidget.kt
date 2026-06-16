package com.clockin.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition

/** 2×2 紧凑 */
class CompactPanelWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        WidgetSnapshotStore.refreshFromRepository(context.applicationContext, appWidgetId)
        provideContent {
            WidgetSnapshotContent(appWidgetId = appWidgetId)
        }
    }
}

@Composable
private fun WidgetSnapshotContent(appWidgetId: Int) {
    val context = LocalContext.current
    val prefs = currentState<Preferences>()
    val refreshTick = prefs[WidgetKeys.REFRESH_TICK] ?: 0L
    val state = remember(refreshTick) {
        WidgetSnapshotStore.readSync(context, appWidgetId)
            ?: WidgetUiState(appWidgetId = appWidgetId)
    }
    WidgetScaffold {
        WidgetStandardContent(state, WidgetLayoutStyle.COMPACT)
    }
}

class CompactPanelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactPanelWidget()
}
