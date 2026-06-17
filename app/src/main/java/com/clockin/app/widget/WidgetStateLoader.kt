package com.clockin.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.clockin.app.ClockInApplication
import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.CycleCalculator
import com.clockin.app.domain.DateFormats
import com.clockin.app.domain.PayCycle
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.StatsCalculator
import com.clockin.app.domain.toLocalDate
import com.clockin.app.domain.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

data class WidgetUiState(
    val shiftDateLabel: String = "",
    val clockInText: String = "--:--",
    val clockOutText: String = "--:--",
    val hasClockedIn: Boolean = false,
    val hasClockedOut: Boolean = false,
    val canClockIn: Boolean = false,
    val canClockOut: Boolean = false,
    val cycleLabel: String = "",
    val clockedDays: Int = 0,
    val totalHours: Double = 0.0,
    val targetDays: Int = 0,
    val targetHours: Float = 0f,
    val remainingDays: Int = 0,
    val remainingHours: Double = 0.0,
    val requiredDailyByTargetDays: Double? = null,
    val requiredDailyByTargetDaysUnreachable: Boolean = false,
    val requiredDailyByCycleDays: Double? = null,
    val requiredDailyByCycleDaysUnreachable: Boolean = false,
    val daysTargetMetHoursNotMet: Boolean = false,
    val hoursWorkedToday: Double? = null,
    val appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
)

object WidgetStateLoader {
    private val shiftDateLabelFormat = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)

    suspend fun load(context: Context): WidgetUiState {
        val repository = (context.applicationContext as ClockInApplication).repository
        val settings = repository.settings.first()
        val cycle = CycleCalculator.currentCycle(settings.cycleStartDay)
        val (shiftDate, record) = repository.loadActiveShift()
        val cycleRecords = repository.getCycleRecords(cycle)
        return buildState(
            settings = settings,
            activeShift = shiftDate to record,
            cycleRecords = cycleRecords,
            cycle = cycle,
        )
    }

    fun buildState(
        settings: AppSettings,
        activeShift: Pair<String, ClockRecord?>,
        cycleRecords: List<ClockRecord>,
        cycle: PayCycle,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    ): WidgetUiState {
        val (shiftDate, record) = activeShift
        val detail = record?.let { ShiftCalculator.buildRecordDetail(it, settings) }
        val progress = StatsCalculator.targetProgress(cycleRecords, settings, cycle)
        val hasClockedIn = record?.clockInTime != null
        val hasClockedOut = record?.clockOutTime != null
        return WidgetUiState(
            shiftDateLabel = shiftDate.toLocalDate().format(shiftDateLabelFormat),
            clockInText = formatClockTime(record?.clockInTime),
            clockOutText = formatClockTime(record?.clockOutTime),
            hasClockedIn = hasClockedIn,
            hasClockedOut = hasClockedOut,
            canClockIn = !hasClockedIn,
            canClockOut = hasClockedIn,
            cycleLabel = cycle.label(),
            clockedDays = progress.clockedDays,
            totalHours = progress.totalHours,
            targetDays = progress.targetDays,
            targetHours = progress.targetHours,
            remainingDays = progress.remainingDays,
            remainingHours = progress.remainingHours,
            requiredDailyByTargetDays = progress.requiredDailyByTargetDays,
            requiredDailyByTargetDaysUnreachable = progress.requiredDailyByTargetDaysUnreachable,
            requiredDailyByCycleDays = progress.requiredDailyByCycleDays,
            requiredDailyByCycleDaysUnreachable = progress.requiredDailyByCycleDaysUnreachable,
            daysTargetMetHoursNotMet = progress.daysTargetMetHoursNotMet,
            hoursWorkedToday = detail?.hoursWorked,
            appWidgetId = appWidgetId,
        )
    }

    fun todayCompactLine(state: WidgetUiState): String {
        val inPart = if (state.hasClockedIn) {
            "上${state.clockInText}✓"
        } else {
            "上未打"
        }
        val outPart = when {
            state.hasClockedOut -> "下${state.clockOutText}✓"
            state.hasClockedIn -> "下未打"
            else -> "下--"
        }
        val base = "$inPart  $outPart"
        val hours = state.hoursWorkedToday?.let { ShiftCalculator.formatHoursShort(it) }
        return if (hours != null) "$base · $hours" else base
    }

    fun remainingFootnote(state: WidgetUiState): String {
        val days = StatsCalculator.formatRemainingDays(state.remainingDays)
        val hours = StatsCalculator.formatRemainingHours(state.remainingHours)
        return "剩 $days · $hours"
    }

    /** 两种剩余所需日均：按目标出勤 / 按周期日历 */
    fun requiredDailyFootnote(state: WidgetUiState): String {
        val byCycle = StatsCalculator.formatDailyAvg(
            state.requiredDailyByCycleDays,
            state.requiredDailyByCycleDaysUnreachable,
        )
        if (state.daysTargetMetHoursNotMet) {
            val remainingHours = StatsCalculator.formatRemainingHours(state.remainingHours)
            return "剩余所需 出勤$remainingHours · 日历日均 $byCycle"
        }
        val byTarget = StatsCalculator.formatDailyAvg(
            state.requiredDailyByTargetDays,
            state.requiredDailyByTargetDaysUnreachable,
        )
        return "剩余所需日均 出勤$byTarget · 日历$byCycle"
    }

    fun progressFootnote(state: WidgetUiState): String =
        "${state.clockedDays}/${state.targetDays}天 · " +
            "${"%.0f".format(state.totalHours)}/${state.targetHours.toInt()}h · " +
            "日均${requiredDailySummary(state)}"

    fun cycleSummary(state: WidgetUiState): String =
        "${state.clockedDays}天 · ${"%.1f".format(state.totalHours)}h"

    fun clockInStatusText(state: WidgetUiState): String =
        if (state.hasClockedIn) "${state.clockInText} ✓" else "未打卡"

    fun clockOutStatusText(state: WidgetUiState): String = when {
        state.hasClockedOut -> "${state.clockOutText} ✓"
        state.hasClockedIn -> "未打卡"
        else -> "--:--"
    }

    fun targetDaysSummary(state: WidgetUiState): String =
        "${state.clockedDays} / ${state.targetDays} 天"

    fun targetHoursSummary(state: WidgetUiState): String =
        "${"%.1f".format(state.totalHours)} / ${state.targetHours} h"

    fun remainingSummary(state: WidgetUiState): String =
        "${StatsCalculator.formatRemainingDays(state.remainingDays)} · " +
            StatsCalculator.formatRemainingHours(state.remainingHours)

    fun requiredDailySummary(progress: WidgetUiState): String {
        if (progress.daysTargetMetHoursNotMet) {
            return "还需 ${StatsCalculator.formatRemainingHours(progress.remainingHours)}"
        }
        return StatsCalculator.formatDailyAvg(
            progress.requiredDailyByCycleDays,
            progress.requiredDailyByCycleDaysUnreachable,
        )
    }

    private fun formatClockTime(millis: Long?): String {
        if (millis == null) return "--:--"
        return millis.toLocalDateTime().format(DateFormats.TIME)
    }
}
