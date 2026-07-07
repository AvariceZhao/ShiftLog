package com.clockin.app.update

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApkUpdateDownloaderTest {
    @Test
    fun validateApkFile_acceptsZipMagic() {
        val file = tempZipFile()
        ApkUpdateDownloader.validateApkFile(file)
    }

    @Test
    fun validateApkFile_rejectsHtmlLikeFile() {
        val file = File.createTempFile("invalid", ".apk")
        file.writeText("<html>" + "x".repeat(2000) + "</html>")
        val error = runCatching {
            ApkUpdateDownloader.validateApkFile(file)
        }.exceptionOrNull()
        assertTrue(error?.message?.contains("格式无效") == true)
    }

    @Test
    fun validateApkFile_rejectsIncompleteDownload() {
        val file = tempZipFile()
        val error = runCatching {
            ApkUpdateDownloader.validateApkFile(file, expectedSizeBytes = file.length() + 10_000L)
        }.exceptionOrNull()
        assertTrue(error?.message?.contains("不完整") == true)
    }

    private fun tempZipFile(): File {
        val file = File.createTempFile("valid", ".apk")
        file.outputStream().use { out ->
            out.write(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
            out.write(ByteArray(2048) { index -> (index * 17 + 31).toByte() })
        }
        return file
    }
}
