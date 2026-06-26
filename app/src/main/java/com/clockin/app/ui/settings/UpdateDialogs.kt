package com.clockin.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clockin.app.ui.components.SimpleMarkdownText
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.TextSecondary
import com.clockin.app.update.ApkInstallHelper
import com.clockin.app.update.ApkUpdateDownloader
import com.clockin.app.update.ReleaseInfo
import java.io.File
import kotlinx.coroutines.launch

private sealed interface ApkDownloadUiState {
    data object Idle : ApkDownloadUiState
    data class Downloading(val progress: Float) : ApkDownloadUiState
    data class Ready(val file: File) : ApkDownloadUiState
    data class Failed(val message: String) : ApkDownloadUiState
}

@Composable
fun UpdateAvailableDialog(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
    onDismissVersion: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloader = remember { ApkUpdateDownloader(context) }
    var downloadState by remember(release.tagName) { mutableStateOf<ApkDownloadUiState>(ApkDownloadUiState.Idle) }
    var statusHint by remember(release.tagName) { mutableStateOf<String?>(null) }

    fun openReleasePage() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
    }

    fun startInstall(file: File) {
        if (!ApkInstallHelper.canInstallPackages(context)) {
            statusHint = "请允许安装未知应用后返回继续"
            ApkInstallHelper.openInstallPermissionSettings(context)
            return
        }
        ApkInstallHelper.installApk(context, file)
    }

    fun startDownload() {
        val url = release.apkDownloadUrl
        if (url.isNullOrBlank()) {
            openReleasePage()
            onDismiss()
            return
        }
        if (!ApkInstallHelper.canInstallPackages(context)) {
            statusHint = "请先允许本应用安装更新包"
            ApkInstallHelper.openInstallPermissionSettings(context)
            return
        }
        downloadState = ApkDownloadUiState.Downloading(0f)
        statusHint = null
        scope.launch {
            downloader.download(
                url = url,
                fileName = "ShiftLog-${release.tagName}.apk",
            ) { progress ->
                downloadState = ApkDownloadUiState.Downloading(progress)
            }.onSuccess { file ->
                downloadState = ApkDownloadUiState.Ready(file)
            }.onFailure { error ->
                downloadState = ApkDownloadUiState.Failed(
                    error.message ?: "下载失败，请稍后重试",
                )
            }
        }
    }

    val isDownloading = downloadState is ApkDownloadUiState.Downloading
    val confirmLabel = when (downloadState) {
        is ApkDownloadUiState.Ready -> "立即安装"
        is ApkDownloadUiState.Downloading -> "下载中…"
        is ApkDownloadUiState.Failed -> if (release.hasInAppApk) "重试下载" else "前往下载"
        ApkDownloadUiState.Idle ->
            if (release.hasInAppApk) "下载并安装" else "前往下载"
    }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        containerColor = NightSurface,
        title = {
            Text(
                "发现新版本 v${release.versionName}",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "更新内容",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                SimpleMarkdownText(release.releaseNotes)
                when (val state = downloadState) {
                    is ApkDownloadUiState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "下载中 ${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    is ApkDownloadUiState.Ready -> {
                        Text(
                            "安装包已就绪，点击下方按钮安装",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    is ApkDownloadUiState.Failed -> {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    ApkDownloadUiState.Idle -> {
                        release.apkSizeBytes?.takeIf { it > 0L }?.let { bytes ->
                            Text(
                                "安装包约 ${formatApkSize(bytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
                statusHint?.let { hint ->
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                if (!isDownloading) {
                    Text(
                        "此版本不再提醒",
                        modifier = Modifier.clickable(onClick = onDismissVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (val state = downloadState) {
                        is ApkDownloadUiState.Ready -> startInstall(state.file)
                        is ApkDownloadUiState.Failed, ApkDownloadUiState.Idle -> startDownload()
                        is ApkDownloadUiState.Downloading -> Unit
                    }
                },
                enabled = !isDownloading,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    when (downloadState) {
                        is ApkDownloadUiState.Ready -> openReleasePage()
                        else -> if (!isDownloading) onDismiss()
                    }
                },
                enabled = !isDownloading,
            ) {
                Text(
                    when (downloadState) {
                        is ApkDownloadUiState.Ready -> "浏览器打开"
                        else -> "稍后"
                    },
                )
            }
        },
    )
}

private fun formatApkSize(bytes: Long): String {
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
