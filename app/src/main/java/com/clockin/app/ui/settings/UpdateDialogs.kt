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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.TextSecondary
import com.clockin.app.update.ReleaseInfo

@Composable
fun UpdateAvailableDialog(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
    onDismissVersion: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
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
                Text(
                    release.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Text(
                    "此版本不再提醒",
                    modifier = Modifier.clickable(onClick = onDismissVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
                    onDismiss()
                },
            ) {
                Text("前往下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        },
    )
}
