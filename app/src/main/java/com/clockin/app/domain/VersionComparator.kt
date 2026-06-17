package com.clockin.app.domain

object VersionComparator {
    fun normalize(tagOrVersion: String): String =
        tagOrVersion.trim().removePrefix("v").removePrefix("V")

    /** @return 正数表示 [latest] 比 [current] 新，0 表示相同，负数表示更旧 */
    fun compare(latest: String, current: String): Int {
        val latestParts = parseParts(normalize(latest))
        val currentParts = parseParts(normalize(current))
        val size = maxOf(latestParts.size, currentParts.size, 3)
        for (i in 0 until size) {
            val lv = latestParts.getOrElse(i) { 0 }
            val cv = currentParts.getOrElse(i) { 0 }
            if (lv != cv) return lv.compareTo(cv)
        }
        return 0
    }

    fun isNewer(latest: String, current: String): Boolean = compare(latest, current) > 0

    private fun parseParts(version: String): List<Int> =
        version.split('.', '-', '_')
            .mapNotNull { part -> part.filter(Char::isDigit).toIntOrNull() }
            .ifEmpty { listOf(0) }
}
