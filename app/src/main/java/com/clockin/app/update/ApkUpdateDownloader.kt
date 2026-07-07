package com.clockin.app.update

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ApkUpdateDownloader(
    context: Context,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }

    suspend fun download(
        url: String,
        fileName: String,
        expectedSizeBytes: Long? = null,
        onProgress: suspend (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dest = File(updateDir, fileName)
            if (dest.exists()) dest.delete()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("下载失败（HTTP ${response.code}）")
                }
                val contentType = response.header("Content-Type")?.lowercase().orEmpty()
                if (contentType.contains("text/html")) {
                    error("下载失败：服务器返回了网页而非安装包")
                }
                val body = response.body ?: error("下载内容为空")
                val totalBytes = body.contentLength()
                body.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            val progress = if (totalBytes > 0L) {
                                (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            if (progress >= 0f) {
                                withContext(Dispatchers.Main.immediate) {
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                }
            }
            validateApkFile(dest, expectedSizeBytes)
            withContext(Dispatchers.Main.immediate) { onProgress(1f) }
            dest
        }
    }

    companion object {
        private const val USER_AGENT = "ShiftLog-Android"

        internal fun validateApkFile(file: File, expectedSizeBytes: Long? = null) {
            val size = file.length()
            require(size > 1024L) { "安装包文件过小，可能下载不完整" }

            file.inputStream().use { input ->
                val magic = ByteArray(2)
                require(input.read(magic) == 2) { "无法读取安装包" }
                require(magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte()) {
                    "安装包格式无效，请稍后重试或改用浏览器下载"
                }
            }

            expectedSizeBytes?.takeIf { it > 0L }?.let { expected ->
                val minSize = (expected * 0.95).toLong()
                require(size >= minSize) {
                    "安装包不完整（已下载 ${size / 1024} KB，预期约 ${expected / 1024} KB）"
                }
            }
        }
    }
}
