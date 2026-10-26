package com.clockin.app.holiday

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clockin.app.ClockInApplication
import java.util.concurrent.TimeUnit

class HolidayCalendarSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as ClockInApplication).repository
        return try {
            val refreshed = repository.refreshHolidayCalendar()
            if (refreshed.failedYears.isEmpty()) Result.success() else Result.retry()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object HolidayCalendarSyncScheduler {
    private const val WORK_NAME = "shiftlog_holiday_calendar_sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<HolidayCalendarSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
