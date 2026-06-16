package com.clockin.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CsvExporter {
    fun export(
        cycle: PayCycle,
        records: List<ClockRecord>,
        settings: AppSettings,
        progress: TargetProgress,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# 周期,${cycle.start},${cycle.end}")
        sb.appendLine("# 目标,${progress.targetDays}天,${progress.targetHours}h")
        sb.appendLine("# 已完成,${progress.clockedDays}天,${"%.1f".format(progress.totalHours)}h")
        sb.appendLine("日期,上班时间,下班时间,工时(小时),备注")
        val sorted = records.sortedByDescending { it.shiftDate }
        sorted.forEach { record ->
            val detail = ShiftCalculator.buildRecordDetail(record, settings)
            val clockIn = record.clockInTime?.let {
                ShiftCalculator.formatTime(it).take(5)
            } ?: ""
            val clockOut = record.clockOutTime?.let {
                ShiftCalculator.formatTime(it).take(5)
            } ?: ""
            val hours = detail.hoursWorked?.let { "%.2f".format(it) } ?: ""
            sb.appendLine("${record.shiftDate},$clockIn,$clockOut,$hours,${detail.note}")
        }
        return "\uFEFF" + sb.toString()
    }

    fun fileName(cycle: PayCycle): String {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        return "打卡_${cycle.start.format(fmt)}_${cycle.end.format(fmt)}.csv"
    }
}
