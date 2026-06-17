package com.clockin.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.clockin.app.domain.AppBackup
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun RestoreBackupDialog(
    backup: AppBackup,
    onDismiss: () -> Unit,
    onMerge: () -> Unit,
    onReplaceAll: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightSurface,
        title = { Text("恢复备份", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    buildString {
                        append("共 ${backup.records.size} 条打卡记录")
                        if (backup.exportedAt.isNotBlank()) {
                            append("，导出时间 ${backup.exportedAt}")
                        }
                        append(
                            "。\n\n合并：保留现有数据，同日期记录以备份为准。\n" +
                                "完全覆盖：清空现有记录并恢复备份（含设置）。",
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                TextButton(onClick = onReplaceAll) {
                    Text("完全覆盖（含设置）")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onMerge) {
                Text("合并导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
fun RestoreCsvDialog(
    recordCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightSurface,
        title = { Text("导入 CSV", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "将合并导入 $recordCount 条记录，同日期会以 CSV 内容覆盖。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
