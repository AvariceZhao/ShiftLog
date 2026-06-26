package com.clockin.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportBackup: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = NightSurface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("选择导出格式", style = MaterialTheme.typography.titleLarge)
                ExportFormatOption(
                    title = "CSV · 表格导出",
                    description = "导出全部周期的打卡记录，Excel / 表格可直接打开，适合查看、打印或做表。可通过「恢复」合并导入。",
                    actionLabel = "导出 CSV",
                    onClick = onExportCsv,
                )
                ExportFormatOption(
                    title = "JSON · 完整备份",
                    description = "导出全部周期的打卡记录与应用设置，适合换机、完整备份；恢复时可选择合并或完全覆盖（含设置）。",
                    actionLabel = "导出 JSON",
                    onClick = onExportBackup,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatOption(
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Text(
            actionLabel,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
