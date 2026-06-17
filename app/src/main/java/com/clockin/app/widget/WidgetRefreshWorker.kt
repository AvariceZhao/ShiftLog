package com.clockin.app.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** 后台定时刷新小组件快照与 UI */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!WidgetRefreshScheduler.hasActiveWidgets(applicationContext)) {
            return Result.success()
        }
        WidgetSnapshotStore.refreshFromRepository(applicationContext)
        WidgetRefresh.updateAll(applicationContext)
        return Result.success()
    }
}
