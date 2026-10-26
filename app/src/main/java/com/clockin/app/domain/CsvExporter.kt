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
        val workingRecords = records.filter {
            !it.isLeave && WorkdayCalendar.isWorkday(it.shiftDate.toLocalDate(), settings, it)
        }
        val clockedDays = workingRecords.count { it.clockInTime != null }
        var totalHours = 0.0
        workingRecords.forEach { record ->
            ShiftCalculator.buildRecordDetail(record, settings).hoursWorked?.let {
                totalHours += it
            }
        }
        sb.appendLine("# 范围,全部周期")
        sb.appendLine("# 记录数,${records.size}")
        sb.appendLine("# 目标,${settings.targetDays}天,${settings.targetHours}h")
        sb.appendLine("# 合计,${clockedDays}天,${"%.1f".format(totalHours)}h")
        sb.appendLine("日期,上班时间,下班时间,工时(小时),备注,排班")
        records.sortedByDescending { it.shiftDate }.forEach { record ->
            val detail = ShiftCalculator.buildRecordDetail(record, settings)
            val isWorkday = WorkdayCalendar.isWorkday(record.shiftDate.toLocalDate(), settings, record)
            val clockIn = record.clockInTime?.let { formatTime(it) } ?: ""
            val clockOut = record.clockOutTime?.let { formatTime(it) } ?: ""
            val hours = detail.hoursWorked?.takeIf { isWorkday && !record.isLeave }
                ?.let { "%.2f".format(it) } ?: ""
            sb.appendLine(
                listOf(
                    formatDate(record.shiftDate),
                    clockIn,
                    clockOut,
                    hours,
                    when {
                        record.isLeave -> "请假"
                        !isWorkday -> "休息"
                        else -> detail.note
                    },
                    when (record.daySchedule) {
                        DaySchedule.AUTO -> "跟随日历"
                        DaySchedule.WORKDAY -> "工作日"
                        DaySchedule.REST_DAY -> "休息日"
                    },
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
