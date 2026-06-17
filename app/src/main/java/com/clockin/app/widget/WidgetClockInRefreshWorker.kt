package com.clockin.app.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clockin.app.ClockInApplication

/** 无精确闹钟权限时，在标准上班时间近似触发刷新 */
class WidgetClockInRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!WidgetRefreshScheduler.hasActiveWidgets(applicationContext)) {
            return Result.success()
        }
        val app = applicationContext as ClockInApplication
        app.refreshWidgetsNow()
        WidgetClockInAlarmScheduler.schedule(applicationContext)
        return Result.success()
    }
}
