package com.clockin.app.domain

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppBackup(
    val version: Int,
    val exportedAt: String,
    val settings: AppSettings,
    val records: List<ClockRecord>,
)

object BackupExporter {
    private const val FORMAT_VERSION = 1

    fun export(settings: AppSettings, records: List<ClockRecord>): String {
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", Instant.now().toString())
        root.put("settings", settingsToJson(settings))
        root.put("records", recordsToJson(records))
        return root.toString(2)
    }

    fun fileName(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
            .withZone(zoneId)
            .format(Instant.now())
        return "ShiftLog_backup_$stamp.json"
    }

    private fun settingsToJson(settings: AppSettings): JSONObject = JSONObject().apply {
        put("standardClockIn", settings.standardClockIn.format(DateFormats.TIME))
        put("standardClockOut", settings.standardClockOut.format(DateFormats.TIME))
        put("isClockOutNextDay", settings.isClockOutNextDay)
        put("cycleStartDay", settings.cycleStartDay)
        put("targetDays", settings.targetDays)
        put("targetHours", settings.targetHours.toDouble())
        put("calendarHoursDecimalPlaces", settings.calendarHoursDecimalPlaces)
        put("showExpectedClockOut", settings.showExpectedClockOut)
        put("showElapsedWorkTime", settings.showElapsedWorkTime)
        put("showPunchStatus", settings.showPunchStatus)
        put("showHomeTargetProgress", settings.showHomeTargetProgress)
        put("showHistoryCycleSummary", settings.showHistoryCycleSummary)
        put("useWorkdayCalendar", settings.useWorkdayCalendar)
        put("targetProgressMode", settings.targetProgressMode.name)
        put("attendanceTiers", JSONArray().apply {
            settings.attendanceTiers.sortedBy { it.minimumHours }.forEach { tier ->
                put(JSONObject().apply {
                    put("minimumHours", tier.minimumHours.toDouble())
                    put("attendanceCredit", tier.attendanceCredit.toDouble())
                    put("absenceCredit", tier.absenceCredit.toDouble())
                })
            }
        })
    }

    private fun recordsToJson(records: List<ClockRecord>): JSONArray {
        val array = JSONArray()
        records.sortedBy { it.shiftDate }.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("shiftDate", record.shiftDate)
                    put("clockInTime", record.clockInTime ?: JSONObject.NULL)
                    put("clockOutTime", record.clockOutTime ?: JSONObject.NULL)
                    put("isLeave", record.isLeave)
                    put("daySchedule", record.daySchedule.name)
                },
            )
        }
        return array
    }
}

object BackupImporter {
    fun parseJson(text: String): Result<AppBackup> = runCatching {
        val root = JSONObject(text.trim())
        val version = root.getInt("version")
        require(version == 1) { "不支持的备份版本：$version" }
        val settings = parseSettings(root.getJSONObject("settings"))
        val records = parseRecords(root.getJSONArray("records"))
        AppBackup(
            version = version,
            exportedAt = root.optString("exportedAt"),
            settings = settings,
            records = records,
        )
    }.recoverCatching { error ->
        throw IllegalArgumentException(error.message ?: "备份文件格式无效")
    }

    /** 解析 App 导出的 CSV（含 # 注释行与表头） */
    fun parseCsv(
        text: String,
        settings: AppSettings,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Result<List<ClockRecord>> = runCatching {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        val dataLines = lines.dropWhile { !it.startsWith("日期,") && !it.startsWith("日期，") }
        if (dataLines.isEmpty()) error("未找到 CSV 数据行")
        dataLines.drop(1).mapNotNull { line ->
            runCatching { parseCsvLine(line, settings, zoneId) }.getOrNull()
        }
    }.recoverCatching { error ->
        throw IllegalArgumentException(error.message ?: "CSV 文件格式无效")
    }

    private fun parseSettings(json: JSONObject): AppSettings = AppSettings(
        standardClockIn = LocalTime.parse(json.getString("standardClockIn"), DateFormats.TIME),
        standardClockOut = LocalTime.parse(json.getString("standardClockOut"), DateFormats.TIME),
        isClockOutNextDay = json.getBoolean("isClockOutNextDay"),
        cycleStartDay = json.getInt("cycleStartDay").coerceIn(1, 28),
        targetDays = json.getInt("targetDays").coerceAtLeast(1),
        targetHours = json.getDouble("targetHours").toFloat().coerceAtLeast(0f),
        calendarHoursDecimalPlaces = json.optInt("calendarHoursDecimalPlaces", 1).coerceIn(1, 2),
        showExpectedClockOut = json.optBoolean("showExpectedClockOut", true),
        showElapsedWorkTime = json.optBoolean("showElapsedWorkTime", true),
        showPunchStatus = json.optBoolean("showPunchStatus", true),
        showHomeTargetProgress = json.optBoolean("showHomeTargetProgress", true),
        showHistoryCycleSummary = json.optBoolean("showHistoryCycleSummary", true),
        useWorkdayCalendar = json.optBoolean("useWorkdayCalendar", true),
        targetProgressMode = json.optString(
            "targetProgressMode",
            TargetProgressMode.EFFECTIVE_ATTENDANCE.name,
        ).let {
            runCatching { TargetProgressMode.valueOf(it) }
                .getOrDefault(TargetProgressMode.EFFECTIVE_ATTENDANCE)
        },
        attendanceTiers = parseAttendanceTiers(json.optJSONArray("attendanceTiers")),
    )

    private fun parseRecords(array: JSONArray): List<ClockRecord> {
        val records = mutableListOf<ClockRecord>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val shiftDate = item.getString("shiftDate")
            records += ClockRecord(
                shiftDate = shiftDate,
                clockInTime = if (item.isNull("clockInTime")) null else item.getLong("clockInTime"),
                clockOutTime = if (item.isNull("clockOutTime")) null else item.getLong("clockOutTime"),
                isLeave = item.optBoolean("isLeave", false),
                daySchedule = runCatching {
                    DaySchedule.valueOf(item.optString("daySchedule", DaySchedule.AUTO.name))
                }.getOrDefault(DaySchedule.AUTO),
            )
        }
        return records
    }

    private fun parseAttendanceTiers(array: JSONArray?): List<AttendanceTier> {
        if (array == null) return DefaultAttendanceTiers
        val tiers = mutableListOf<AttendanceTier>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val hours = item.optDouble("minimumHours", -1.0).toFloat()
            val credit = item.optDouble("attendanceCredit", -1.0).toFloat()
            val absence = item.optDouble("absenceCredit", 0.0).toFloat()
            AttendanceTier(hours, credit, absence).takeIf { it.isValid }?.let(tiers::add)
        }
        return tiers.distinctBy { it.minimumHours }.sortedBy { it.minimumHours }
            .ifEmpty { DefaultAttendanceTiers }
    }

    private fun parseCsvLine(
        line: String,
        settings: AppSettings,
        zoneId: ZoneId,
    ): ClockRecord? {
        val parts = splitCsvLine(line)
        if (parts.size < 3) return null
        val shiftDate = normalizeDate(parts[0]) ?: return null
        val date = shiftDate.toLocalDate()
        val clockIn = parts[1].takeIf { it.isNotBlank() }?.let { parseTime(it) }
        val clockOut = parts[2].takeIf { it.isNotBlank() }?.let { parseTime(it) }
        val isLeave = parts.getOrNull(4)?.trim() == "请假"
        val daySchedule = when (parts.getOrNull(5)?.trim()) {
            "工作日" -> DaySchedule.WORKDAY
            "休息日" -> DaySchedule.REST_DAY
            else -> DaySchedule.AUTO
        }
        if (clockIn == null && clockOut == null && !isLeave && daySchedule == DaySchedule.AUTO) return null
        if (parts[1].isNotBlank() && clockIn == null) return null
        if (parts[2].isNotBlank() && clockOut == null) return null
        return ClockRecord(
            shiftDate = shiftDate,
            clockInTime = clockIn?.let { timeToMillis(date, it, isClockOut = false, settings, zoneId) },
            clockOutTime = clockOut?.let { timeToMillis(date, it, isClockOut = true, settings, zoneId) },
            isLeave = isLeave,
            daySchedule = daySchedule,
        )
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        line.forEach { ch ->
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result += current.toString()
        return result
    }

    private fun normalizeDate(raw: String): String? {
        val text = raw.trim()
        val parsers = listOf(
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        )
        for (fmt in parsers) {
            runCatching { return java.time.LocalDate.parse(text, fmt).toShiftDateString() }
        }
        return null
    }

    private fun parseTime(text: String): LocalTime? {
        val trimmed = text.trim()
        val formatters = listOf(
            DateFormats.TIME,
            DateFormats.TIME_SECONDS,
            java.time.format.DateTimeFormatter.ofPattern("H:mm"),
            java.time.format.DateTimeFormatter.ofPattern("H:mm:ss"),
        )
        for (fmt in formatters) {
            runCatching { return LocalTime.parse(trimmed, fmt) }.getOrNull()
        }
        return null
    }

    private fun timeToMillis(
        shiftDate: java.time.LocalDate,
        time: LocalTime,
        isClockOut: Boolean,
        settings: AppSettings,
        zoneId: ZoneId,
    ): Long {
        var date = shiftDate
        if (isClockOut && settings.isClockOutNextDay && !time.isAfter(settings.standardClockOut)) {
            date = shiftDate.plusDays(1)
        }
        return java.time.LocalDateTime.of(date, time).atZone(zoneId).toInstant().toEpochMilli()
    }
}
