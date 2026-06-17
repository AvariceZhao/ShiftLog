package com.clockin.app

import android.app.Application
import com.clockin.app.data.AppDatabase
import com.clockin.app.data.ClockRepository
import com.clockin.app.data.SettingsRepository
import com.clockin.app.update.UpdateCoordinator
import com.clockin.app.widget.WidgetClockInAlarmScheduler
import com.clockin.app.widget.WidgetRefresh
import com.clockin.app.widget.WidgetRefreshScheduler
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

    lateinit var updateCoordinator: UpdateCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        updateCoordinator = UpdateCoordinator(this)
        val db = AppDatabase.create(this)
        val settingsRepository = SettingsRepository(this)
        repository = ClockRepository(
            dao = db.clockRecordDao(),
            settingsRepository = settingsRepository,
            db = db,
            onRecordsChanged = { refreshWidgets() },
            onSettingsChanged = { scheduleWidgetRefreshJobs() },
        )
        scheduleWidgetRefreshJobs()
        applicationScope.launch {
            updateCoordinator.checkForUpdate(manual = false)
        }
    }

    fun scheduleWidgetRefreshJobs() {
        applicationScope.launch {
            WidgetRefreshScheduler.schedule(this@ClockInApplication)
            WidgetClockInAlarmScheduler.schedule(this@ClockInApplication)
        }
    }

    fun cancelWidgetRefreshJobs() {
        WidgetRefreshScheduler.cancel(this)
        WidgetClockInAlarmScheduler.cancel(this)
    }

    fun refreshWidgets(appWidgetId: Int = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
        applicationScope.launch {
            refreshWidgetsNow(appWidgetId)
            delay(1500)
            refreshWidgetsNow(appWidgetId)
        }
    }

    suspend fun refreshWidgetsNow(appWidgetId: Int = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
        WidgetSnapshotStore.refreshFromRepository(this@ClockInApplication, appWidgetId)
        WidgetRefresh.updateAll(this@ClockInApplication, appWidgetId)
    }
}
