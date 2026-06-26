package com.clockin.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    /** Excel 打开 CSV 时会把 `2026-06-16` 当成减法；斜杠格式可正常识别为日期 */
    private val EXCEL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    fun export(
        records: List<ClockRecord>,
        settings: AppSettings,
    ): String {
        val sb = StringBuilder()
        val clockedDays = records.count { it.clockInTime != null }
        var totalHours = 0.0
        records.forEach { record ->
            ShiftCalculator.buildRecordDetail(record, settings).hoursWorked?.let {
                totalHours += it
            }
        }
        sb.appendLine("# 范围,全部周期")
        sb.appendLine("# 记录数,${records.size}")
        sb.appendLine("# 目标,${settings.targetDays}天,${settings.targetHours}h")
        sb.appendLine("# 合计,${clockedDays}天,${"%.1f".format(totalHours)}h")
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

    fun fileName(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
            .withZone(zoneId)
            .format(Instant.now())
        return "ShiftLog_打卡_$stamp.csv"
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
