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
    }

    private fun recordsToJson(records: List<ClockRecord>): JSONArray {
        val array = JSONArray()
        records.sortedBy { it.shiftDate }.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("shiftDate", record.shiftDate)
                    put("clockInTime", record.clockInTime ?: JSONObject.NULL)
                    put("clockOutTime", record.clockOutTime ?: JSONObject.NULL)
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
            )
        }
        return records
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
        if (clockIn == null && clockOut == null) return null
        if (parts[1].isNotBlank() && clockIn == null) return null
        if (parts[2].isNotBlank() && clockOut == null) return null
        return ClockRecord(
            shiftDate = shiftDate,
            clockInTime = clockIn?.let { timeToMillis(date, it, isClockOut = false, settings, zoneId) },
            clockOutTime = clockOut?.let { timeToMillis(date, it, isClockOut = true, settings, zoneId) },
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
