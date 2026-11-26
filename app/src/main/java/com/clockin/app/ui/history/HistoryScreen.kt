package com.clockin.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.AppBackup
import com.clockin.app.domain.ClockRecord
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.components.AppIcons
import com.clockin.app.ui.components.ScreenBackground
import com.clockin.app.ui.components.SectionHeader
import com.clockin.app.ui.components.CycleSummaryCard
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.ButtonShape
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun HistoryScreen(
    repository: ClockRepository,
    modifier: Modifier = Modifier,
    importText: String? = null,
    onImportConsumed: () -> Unit = {},
    onExportCsv: (content: String, fileName: String) -> Unit,
    onExportBackup: (content: String, fileName: String) -> Unit,
    onPickImport: () -> Unit,
) {
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ClockRecord?>(null) }
    var pendingBackup by remember { mutableStateOf<AppBackup?>(null) }
    var pendingCsvRecords by remember { mutableStateOf<List<ClockRecord>?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(importText) {
        val text = importText ?: return@LaunchedEffect
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) {
            viewModel.parseBackupJson(
                text = trimmed,
                onReady = { pendingBackup = it },
                onError = { toastMessage = it },
            )
        } else {
            viewModel.parseCsvImport(
                text = trimmed,
                onReady = { records ->
                    if (records.isEmpty()) {
                        toastMessage = "CSV 中没有可导入的记录"
                    } else {
                        pendingCsvRecords = records
                    }
                },
                onError = { toastMessage = it },
            )
        }
        onImportConsumed()
    }

    ScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            SectionHeader(
                title = "历史记录",
                subtitle = state.cycle?.label(),
                icon = AppIcons.History,
            )

            AppCard(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = viewModel::previousCycle) {
                        Icon(AppIcons.ChevronLeft, contentDescription = "上一周期")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            AppIcons.Calendar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            state.cycle?.label() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    IconButton(onClick = viewModel::nextCycle) {
                        Icon(AppIcons.ChevronRight, contentDescription = "下一周期")
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (settings.showHistoryCycleSummary) {
                    state.attendanceStats?.let { stats ->
                        item { CycleSummaryCard(stats) }
                    }
                }

                state.cycle?.let { cycle ->
                    item {
                        CycleCalendarCard(
                            cycle = cycle,
                            records = state.records.map { it.record },
                            settings = settings,
                            onDayClick = { day ->
                                editing = recordOrPlaceholder(
                                    day,
                                    state.records.map { it.record },
                                )
                            },
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = viewModel::goToCurrentCycle,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("回到当前周期")
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { editing = newRecordForToday() },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(AppIcons.Add, contentDescription = null)
                            Text(" 补录", modifier = Modifier.padding(start = 4.dp))
                        }
                        Button(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        ) {
                            Icon(AppIcons.Export, contentDescription = null)
                            Text(" 导出", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onPickImport,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("恢复")
                    }
                }

                toastMessage?.let { message ->
                    item {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }
    }

    editing?.let { record ->
        RecordEditDialog(
            record = record,
            settings = settings,
            onDismiss = { editing = null },
            onSave = {
                viewModel.saveRecord(it)
                editing = null
            },
            onDelete = {
                viewModel.deleteRecord(record.shiftDate)
                editing = null
            },
        )
    }

    pendingBackup?.let { backup ->
        RestoreBackupDialog(
            backup = backup,
            onDismiss = { pendingBackup = null },
            onMerge = {
                viewModel.restoreBackup(backup, replaceExisting = false) { message ->
                    toastMessage = message
                    pendingBackup = null
                }
            },
            onReplaceAll = {
                viewModel.restoreBackup(backup, replaceExisting = true) { message ->
                    toastMessage = message
                    pendingBackup = null
                }
            },
        )
    }

    pendingCsvRecords?.let { records ->
        RestoreCsvDialog(
            recordCount = records.size,
            onDismiss = { pendingCsvRecords = null },
            onConfirm = {
                viewModel.mergeCsvRecords(records) { message ->
                    toastMessage = message
                    pendingCsvRecords = null
                }
            },
        )
    }

    if (showExportDialog) {
        ExportFormatDialog(
            onDismiss = { showExportDialog = false },
            onExportCsv = {
                showExportDialog = false
                viewModel.buildExport(onExportCsv)
            },
            onExportBackup = {
                showExportDialog = false
                viewModel.buildBackup(onExportBackup)
            },
        )
    }
}
