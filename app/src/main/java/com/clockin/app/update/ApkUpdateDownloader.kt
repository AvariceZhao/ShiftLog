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
            withContext(Dispatchers.Main.immediate) { onProgress(1f) }
            dest
        }
    }

    companion object {
        private const val USER_AGENT = "ShiftLog-Android"
    }
}
