package com.clockin.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.components.ScreenBackground
import com.clockin.app.ui.components.SectionHeader
import com.clockin.app.ui.components.StatusChip
import com.clockin.app.ui.components.TargetProgressCard
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.ButtonShape
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun HistoryScreen(
    repository: ClockRepository,
    modifier: Modifier = Modifier,
    onExport: (content: String, fileName: String) -> Unit,
) {
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ClockRecord?>(null) }

    ScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            SectionHeader(
                title = "历史记录",
                subtitle = state.cycle?.label(),
                icon = Icons.Outlined.History,
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
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上一周期")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            state.cycle?.label() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    IconButton(onClick = viewModel::nextCycle) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下一周期")
                    }
                }
            }

            OutlinedButton(
                onClick = viewModel::goToCurrentCycle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text("回到当前周期")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.stats?.let { stats ->
                    item {
                        AppCard {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("周期统计", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "${stats.clockedDays} 天 · ${"%.1f".format(stats.totalHours)}h",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("迟到 ${stats.lateCount}", color = TextSecondary)
                                    Text("早退 ${stats.earlyCount}", color = TextSecondary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("漏上班 ${stats.missedInCount}", color = TextSecondary)
                                    Text("漏下班 ${stats.missedOutCount}", color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                state.progress?.let { progress ->
                    item { TargetProgressCard(progress) }
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
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text(" 补录", modifier = Modifier.padding(start = 4.dp))
                        }
                        Button(
                            onClick = { viewModel.buildExport(onExport) },
                            modifier = Modifier.weight(1f),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null)
                            Text(" 导出", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                if (state.records.isEmpty()) {
                    item {
                        AppCard {
                            Text(
                                "本周期暂无记录",
                                modifier = Modifier.padding(24.dp),
                                color = TextSecondary,
                            )
                        }
                    }
                }

                items(state.records, key = { it.record.shiftDate }) { detail ->
                    AppCard(
                        modifier = Modifier.clickable { editing = detail.record },
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    detail.record.shiftDate,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                detail.hoursWorked?.let {
                                    Text(
                                        "${"%.1f".format(it)}h",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                            Text(
                                "${ShiftCalculator.formatTime(detail.record.clockInTime).take(5)} → " +
                                    ShiftCalculator.formatTime(detail.record.clockOutTime).take(5),
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                            )
                            if (detail.note.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    detail.note.split(";").filter { it.isNotBlank() }.forEach { tag ->
                                        val status = when (tag) {
                                            "迟到" -> com.clockin.app.domain.PunchStatus.LATE
                                            "早退" -> com.clockin.app.domain.PunchStatus.EARLY
                                            else -> com.clockin.app.domain.PunchStatus.MISSED_OUT
                                        }
                                        StatusChip(tag, status)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { record ->
        RecordEditDialog(
            record = record,
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
}
