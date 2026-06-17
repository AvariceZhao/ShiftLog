package com.clockin.app.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    /** Excel 打开 CSV 时会把 `2026-06-16` 当成减法；斜杠格式可正常识别为日期 */
    private val EXCEL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    fun export(
        cycle: PayCycle,
        records: List<ClockRecord>,
        settings: AppSettings,
        progress: TargetProgress,
    ): String {
        val sb = StringBuilder()
        sb.appendLine(
            listOf(
                "# 周期",
                formatDate(cycle.start),
                formatDate(cycle.end),
            ).joinCsvRow(),
        )
        sb.appendLine("# 目标,${progress.targetDays}天,${progress.targetHours}h")
        sb.appendLine("# 已完成,${progress.clockedDays}天,${"%.1f".format(progress.totalHours)}h")
        sb.appendLine("日期,上班时间,下班时间,工时(小时),备注")
        records.sortedByDescending { it.shiftDate }.forEach { record ->
            val detail = ShiftCalculator.buildRecordDetail(record, settings)
            val clockIn = record.clockInTime?.let { formatTime(it) } ?: ""
            val clockOut = record.clockOutTime?.let { formatTime(it) } ?: ""
            val hours = detail.hoursWorked?.let { "%.2f".format(it) } ?: ""
            sb.appendLine(
                listOf(
                    formatDate(record.shiftDate),
                    clockIn,
                    clockOut,
                    hours,
                    detail.note,
                ).joinCsvRow(),
            )
        }
        return "\uFEFF" + sb.toString()
    }

    fun fileName(cycle: PayCycle): String {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        return "打卡_${cycle.start.format(fmt)}_${cycle.end.format(fmt)}.csv"
    }

    private fun formatDate(isoDate: String): String =
        isoDate.toLocalDate().format(EXCEL_DATE)

    private fun formatDate(date: LocalDate): String =
        date.format(EXCEL_DATE)

    private fun formatTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        millis.toLocalDateTime(zoneId).format(DateFormats.TIME)

    private fun List<String>.joinCsvRow(): String =
        joinToString(",") { escapeCsv(it) }

    private fun escapeCsv(value: String): String {
        if (value.isEmpty()) return value
        return if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
