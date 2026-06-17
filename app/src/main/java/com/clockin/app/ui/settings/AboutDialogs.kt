package com.clockin.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clockin.app.ClockInApplication
import com.clockin.app.R
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.TextSecondary
import com.clockin.app.update.ReleaseInfo
import com.clockin.app.update.UpdateCheckResult
import kotlinx.coroutines.launch

private const val GITHUB_URL = "https://github.com/AvariceZhao/ShiftLog"

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ClockInApplication
    val scope = rememberCoroutineScope()
    var showLicense by remember { mutableStateOf(false) }
    var pendingRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var checkHint by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val versionLabel = remember {
        runCatching {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
            "版本 ${info.versionName} ($code)"
        }.getOrDefault("版本 —")
    }

    pendingRelease?.let { release ->
        UpdateAvailableDialog(
            release = release,
            onDismiss = { pendingRelease = null },
            onDismissVersion = {
                scope.launch {
                    app.updateCoordinator.dismissRelease(release.tagName)
                    pendingRelease = null
                }
            },
        )
    }

    if (showLicense) {
        LicenseDialog(onDismiss = { showLicense = false })
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightSurface,
        title = {
            Text("关于 ShiftLog", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    versionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    "作者 AvariceZhao",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "本地打卡记录工具，数据仅存于本机。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    if (checking) "正在检查更新…" else "检查更新",
                    modifier = Modifier.clickable(enabled = !checking) {
                        checking = true
                        checkHint = null
                        scope.launch {
                            when (val result = app.updateCoordinator.checkForUpdate(manual = true)) {
                                is UpdateCheckResult.Available -> pendingRelease = result.release
                                UpdateCheckResult.UpToDate -> checkHint = "当前已是最新版本"
                                is UpdateCheckResult.Failed -> checkHint = result.message
                            }
                            checking = false
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                checkHint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Text(
                    "GitHub 开源项目",
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "查看开源许可 (MIT)",
                    modifier = Modifier.clickable { showLicense = true },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun LicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember {
        context.resources.openRawResource(R.raw.mit_license)
            .bufferedReader()
            .use { it.readText() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightSurface,
        title = {
            Text("开源许可", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    licenseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
