package com.clockin.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.toShiftDateString
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.NightBorder
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.NightSurfaceHigh
import com.clockin.app.ui.theme.StatusMissed
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordEditDialog(
    record: ClockRecord,
    onDismiss: () -> Unit,
    onSave: (ClockRecord) -> Unit,
    onDelete: () -> Unit,
) {
    var shiftDate by remember(record.shiftDate) { mutableStateOf(record.shiftDate) }
    var clockInText by remember(record) {
        mutableStateOf(record.clockInTime?.toTimeText() ?: "")
    }
    var clockOutText by remember(record) {
        mutableStateOf(record.clockOutTime?.toTimeText() ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AmberPrimary,
        unfocusedBorderColor = NightBorder,
        focusedContainerColor = NightSurfaceHigh,
        unfocusedContainerColor = NightSurfaceHigh,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("编辑打卡") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = shiftDate,
                    onValueChange = { shiftDate = it },
                    label = { Text("班次日期") },
                    placeholder = { Text("yyyy-MM-dd") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = clockInText,
                    onValueChange = { clockInText = it },
                    label = { Text("上班时间") },
                    placeholder = { Text("yyyy-MM-dd HH:mm") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = clockOutText,
                    onValueChange = { clockOutText = it },
                    label = { Text("下班时间") },
                    placeholder = { Text("yyyy-MM-dd HH:mm") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true,
                )
                error?.let {
                    Text(it, color = StatusMissed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    runCatching {
                        val date = shiftDate.trim()
                        LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
                        val inMs = clockInText.trim().takeIf { it.isNotEmpty() }?.let { parseDateTime(it) }
                        val outMs = clockOutText.trim().takeIf { it.isNotEmpty() }?.let { parseDateTime(it) }
                        onSave(ClockRecord(date, inMs, outMs))
                    }.onFailure {
                        error = "时间格式错误，请用 yyyy-MM-dd HH:mm"
                    }
                },
            ) {
                Text("保存", color = AmberPrimary)
            }
        },
        dismissButton = {
            Row {
                if (record.clockInTime != null || record.clockOutTime != null) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = StatusMissed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    )
}

fun newRecordForToday(): ClockRecord =
    ClockRecord(LocalDate.now().toShiftDateString(), null, null)

private fun Long.toTimeText(): String {
    val dt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

private fun parseDateTime(text: String): Long {
    val dt = LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
