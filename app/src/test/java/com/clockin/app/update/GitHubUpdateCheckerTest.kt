package com.clockin.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray

class GitHubUpdateCheckerTest {
    @Test
    fun pickApkAsset_prefersArm64Release() {
        val assets = JSONArray(
            """
            [
              {"name": "source.zip", "browser_download_url": "https://example.com/source.zip", "size": 100},
              {"name": "ShiftLog-v1.0.6-arm64-release.apk", "browser_download_url": "https://example.com/arm64.apk", "size": 3000000},
              {"name": "ShiftLog-debug.apk", "browser_download_url": "https://example.com/debug.apk", "size": 4000000}
            ]
            """.trimIndent(),
        )
        val picked = GitHubUpdateChecker.pickApkAsset(assets)
        assertNotNull(picked)
        assertEquals("https://example.com/arm64.apk", picked!!.first)
        assertEquals(3_000_000L, picked.second)
    }

    @Test
    fun pickApkAsset_fallsBackToFirstApk() {
        val assets = JSONArray(
            """
            [{"name": "app.apk", "browser_download_url": "https://example.com/app.apk", "size": 1234}]
            """.trimIndent(),
        )
        val picked = GitHubUpdateChecker.pickApkAsset(assets)
        assertEquals("https://example.com/app.apk", picked?.first)
    }

    @Test
    fun parseReleaseJson_includesApkAsset() {
        val json = """
            {
              "tag_name": "v1.0.6",
              "body": "## 新功能",
              "html_url": "https://github.com/AvariceZhao/ShiftLog/releases/tag/v1.0.6",
              "assets": [
                {
                  "name": "ShiftLog-v1.0.6-arm64-release.apk",
                  "browser_download_url": "https://github.com/download/apk",
                  "size": 5242880
                }
              ]
            }
        """.trimIndent()
        val release = GitHubUpdateChecker.parseReleaseJson(json)
        assertNotNull(release)
        assertEquals("1.0.6", release!!.versionName)
        assertTrue(release.hasInAppApk)
        assertEquals("https://github.com/download/apk", release.apkDownloadUrl)
        assertEquals(5_242_880L, release.apkSizeBytes)
    }

    @Test
    fun parseReleaseJson_withoutApkAsset() {
        val json = """
            {"tag_name": "v1.0.0", "body": "notes", "html_url": "https://example.com", "assets": []}
        """.trimIndent()
        val release = GitHubUpdateChecker.parseReleaseJson(json)
        assertNotNull(release)
        assertNull(release!!.apkDownloadUrl)
    }
}
