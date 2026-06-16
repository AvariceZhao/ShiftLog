package com.clockin.app

import android.app.Application
import com.clockin.app.data.AppDatabase
import com.clockin.app.data.ClockRepository
import com.clockin.app.data.SettingsRepository
import com.clockin.app.widget.WidgetRefresh
import com.clockin.app.widget.WidgetSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClockInApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var repository: ClockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.create(this)
        val settingsRepository = SettingsRepository(this)
        repository = ClockRepository(
            dao = db.clockRecordDao(),
            settingsRepository = settingsRepository,
            onRecordsChanged = { refreshWidgets() },
        )
    }

    fun refreshWidgets(appWidgetId: Int = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
        applicationScope.launch {
            WidgetSnapshotStore.refreshFromRepository(this@ClockInApplication, appWidgetId)
            WidgetRefresh.updateAll(this@ClockInApplication, appWidgetId)
            // 点击小组件后 Glance 会话可能仍活跃，延迟再刷一次
            delay(1500)
            WidgetSnapshotStore.refreshFromRepository(this@ClockInApplication, appWidgetId)
            WidgetRefresh.updateAll(this@ClockInApplication, appWidgetId)
        }
    }
}
