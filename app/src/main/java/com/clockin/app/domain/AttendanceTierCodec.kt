package com.clockin.app.domain

object AttendanceTierCodec {
    fun encode(tiers: List<AttendanceTier>): String = tiers
        .filter { it.isValid }
        .sortedBy { it.minimumHours }
        .joinToString(";") { "${it.minimumHours}:${it.attendanceCredit}:${it.absenceCredit}" }

    fun decode(raw: String?): List<AttendanceTier> {
        if (raw.isNullOrBlank()) return DefaultAttendanceTiers
        val parsed = raw.split(';').mapNotNull { item ->
            val parts = item.split(':')
            if (parts.size !in 2..3) return@mapNotNull null
            val hours = parts[0].toFloatOrNull() ?: return@mapNotNull null
            val attendance = parts[1].toFloatOrNull() ?: return@mapNotNull null
            val absence = if (parts.size == 3) {
                parts[2].toFloatOrNull() ?: return@mapNotNull null
            } else {
                0f
            }
            AttendanceTier(hours, attendance, absence).takeIf { it.isValid }
        }.distinctBy { it.minimumHours }.sortedBy { it.minimumHours }
        return parsed.ifEmpty { DefaultAttendanceTiers }
    }
}
