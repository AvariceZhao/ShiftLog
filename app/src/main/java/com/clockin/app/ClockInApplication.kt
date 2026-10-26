package com.clockin.app

import android.app.Application
import com.clockin.app.data.AppDatabase
import com.clockin.app.data.ClockRepository
import com.clockin.app.data.DataStoreHolidayCalendarStore
import com.clockin.app.data.SettingsRepository
import com.clockin.app.holiday.HolidayCalendarRepository
import com.clockin.app.holiday.HolidayCalendarSyncScheduler
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
        val holidayRepository = HolidayCalendarRepository(
            store = DataStoreHolidayCalendarStore(this),
            onCalendarsChanged = { refreshWidgets() },
        )
        repository = ClockRepository(
            dao = db.clockRecordDao(),
            settingsRepository = settingsRepository,
            db = db,
            holidayRepository = holidayRepository,
            onRecordsChanged = { refreshWidgets() },
            onSettingsChanged = { scheduleWidgetRefreshJobs() },
        )
        scheduleWidgetRefreshJobs()
        HolidayCalendarSyncScheduler.schedule(this)
        refreshHolidayCalendarIfNeeded()
        applicationScope.launch {
            updateCoordinator.checkForUpdate(manual = false)
        }
    }

    fun refreshHolidayCalendarIfNeeded() {
        applicationScope.launch {
            try {
                repository.refreshHolidayCalendar()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.w("HolidayCalendar", "Unable to refresh calendar cache", error)
            }
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
