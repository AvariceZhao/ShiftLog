package com.clockin.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.clockin.app.ClockInApplication
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

object WidgetClockInAlarmScheduler {
    const val ACTION_CLOCK_IN_REFRESH = "com.clockin.app.widget.ACTION_CLOCK_IN_REFRESH"
    private const val REQUEST_CODE = 71001
    private const val WORK_NAME = "shiftlog_widget_clock_in_refresh"

    /** 计算下一次「标准上班时间」触发时刻（毫秒） */
    fun nextTriggerMillis(
        standardClockIn: LocalTime,
        now: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        var target = LocalDateTime.of(now.toLocalDate(), standardClockIn)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.atZone(zoneId).toInstant().toEpochMilli()
    }

    suspend fun schedule(context: Context) {
        val appContext = context.applicationContext
        if (!WidgetRefreshScheduler.hasActiveWidgets(appContext)) {
            cancel(appContext)
            return
        }
        val settings = (appContext as ClockInApplication).repository.settings.first()
        val triggerAt = nextTriggerMillis(settings.standardClockIn)
        scheduleAt(appContext, triggerAt)
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(appContext))
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
    }

    private fun scheduleAt(context: Context, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pending,
            )
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        } else {
            // 无精确闹钟权限时，用 WorkManager 近似触发
            alarmManager.cancel(pending)
            val delayMs = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<WidgetClockInRefreshWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetClockInAlarmReceiver::class.java).apply {
            action = ACTION_CLOCK_IN_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
