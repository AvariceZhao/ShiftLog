package com.clockin.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GitHubUpdateChecker {
    private const val API_URL = "https://api.github.com/repos/AvariceZhao/ShiftLog/releases/latest"
    private const val USER_AGENT = "ShiftLog-Android"

    suspend fun fetchLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""
                if (code !in 200..299) {
                    error("GitHub API 返回 $code")
                }
                parseReleaseJson(body) ?: error("无法解析 Release 信息")
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun parseReleaseJson(json: String): ReleaseInfo? {
        val root = JSONObject(json)
        val tagName = root.optString("tag_name").trim()
        if (tagName.isEmpty()) return null
        val versionName = tagName.removePrefix("v").removePrefix("V")
        val body = root.optString("body").trim().ifEmpty { "暂无更新说明" }
        val pageUrl = root.optString("html_url").trim().ifEmpty {
            "https://github.com/AvariceZhao/ShiftLog/releases/latest"
        }
        return ReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            releaseNotes = body,
            pageUrl = pageUrl,
        )
    }
}
