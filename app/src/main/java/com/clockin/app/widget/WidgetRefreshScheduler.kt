package com.clockin.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetRefreshScheduler {
    private const val WORK_NAME = "shiftlog_widget_periodic_refresh"

    /** 每 2 小时刷新一次（WorkManager 最短周期 15 分钟，此处取用户期望的上限以省电） */
    private val REFRESH_INTERVAL_HOURS = 2L

    fun schedule(context: Context) {
        if (!hasActiveWidgets(context)) return
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            REFRESH_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(WORK_NAME)
    }

    fun hasActiveWidgets(context: Context): Boolean {
        val receiver = ComponentName(context, CompactPanelWidgetReceiver::class.java)
        return AppWidgetManager.getInstance(context)
            .getAppWidgetIds(receiver)
            .isNotEmpty()
    }
}
