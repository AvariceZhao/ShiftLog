package com.clockin.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context

/**
 * 小组件 UI 快照（SharedPreferences 同步读写）。
 * 打卡后立即写入，composition 内按 refreshTick 读取，避免 Glance 里使用 Flow/collectAsState。
 */
object WidgetSnapshotStore {
    private const val PREFS = "widget_snapshot"

    fun saveSync(context: Context, state: WidgetUiState) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("shiftDateLabel", state.shiftDateLabel)
            .putString("clockInText", state.clockInText)
            .putString("clockOutText", state.clockOutText)
            .putBoolean("hasClockedIn", state.hasClockedIn)
            .putBoolean("hasClockedOut", state.hasClockedOut)
            .putBoolean("canClockIn", state.canClockIn)
            .putBoolean("canClockOut", state.canClockOut)
            .putString("cycleLabel", state.cycleLabel)
            .putInt("clockedDays", state.clockedDays)
            .putFloat("totalHours", state.totalHours.toFloat())
            .putInt("targetDays", state.targetDays)
            .putFloat("targetHours", state.targetHours)
            .putInt("remainingDays", state.remainingDays)
            .putFloat("remainingHours", state.remainingHours.toFloat())
            .putFloat("requiredDailyByTargetDays", state.requiredDailyByTargetDays?.toFloat() ?: -1f)
            .putBoolean("requiredDailyByTargetDaysUnreachable", state.requiredDailyByTargetDaysUnreachable)
            .putFloat("requiredDailyByCycleDays", state.requiredDailyByCycleDays?.toFloat() ?: -1f)
            .putBoolean("requiredDailyByCycleDaysUnreachable", state.requiredDailyByCycleDaysUnreachable)
            .putBoolean("daysTargetMetHoursNotMet", state.daysTargetMetHoursNotMet)
            .putFloat("hoursWorkedToday", state.hoursWorkedToday?.toFloat() ?: -1f)
            .putLong("savedAt", System.currentTimeMillis())
            .apply()
    }

    fun readSync(context: Context, appWidgetId: Int): WidgetUiState? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains("savedAt")) return null
        val targetDaily = prefs.getFloat("requiredDailyByTargetDays", -1f)
        val cycleDaily = prefs.getFloat("requiredDailyByCycleDays", -1f)
        val hoursToday = prefs.getFloat("hoursWorkedToday", -1f)
        return WidgetUiState(
            shiftDateLabel = prefs.getString("shiftDateLabel", "") ?: "",
            clockInText = prefs.getString("clockInText", "--:--") ?: "--:--",
            clockOutText = prefs.getString("clockOutText", "--:--") ?: "--:--",
            hasClockedIn = prefs.getBoolean("hasClockedIn", false),
            hasClockedOut = prefs.getBoolean("hasClockedOut", false),
            canClockIn = prefs.getBoolean("canClockIn", true),
            canClockOut = prefs.getBoolean("canClockOut", false),
            cycleLabel = prefs.getString("cycleLabel", "") ?: "",
            clockedDays = prefs.getInt("clockedDays", 0),
            totalHours = prefs.getFloat("totalHours", 0f).toDouble(),
            targetDays = prefs.getInt("targetDays", 0),
            targetHours = prefs.getFloat("targetHours", 0f),
            remainingDays = prefs.getInt("remainingDays", 0),
            remainingHours = prefs.getFloat("remainingHours", 0f).toDouble(),
            requiredDailyByTargetDays = targetDaily.takeIf { it >= 0 }?.toDouble(),
            requiredDailyByTargetDaysUnreachable = prefs.getBoolean(
                "requiredDailyByTargetDaysUnreachable",
                false,
            ),
            requiredDailyByCycleDays = cycleDaily.takeIf { it >= 0 }?.toDouble(),
            requiredDailyByCycleDaysUnreachable = prefs.getBoolean(
                "requiredDailyByCycleDaysUnreachable",
                false,
            ),
            daysTargetMetHoursNotMet = prefs.getBoolean("daysTargetMetHoursNotMet", false),
            hoursWorkedToday = hoursToday.takeIf { it >= 0 }?.toDouble(),
            appWidgetId = appWidgetId,
        )
    }

    suspend fun refreshFromRepository(
        context: Context,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    ): WidgetUiState {
        val appContext = context.applicationContext
        val loaded = WidgetStateLoader.load(appContext)
        val state = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            loaded.copy(appWidgetId = appWidgetId)
        } else {
            loaded
        }
        saveSync(appContext, state)
        return state
    }
}
