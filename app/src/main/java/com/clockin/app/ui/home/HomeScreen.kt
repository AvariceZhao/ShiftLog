package com.clockin.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.ui.components.AppIcons
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.components.ScreenBackground
import com.clockin.app.ui.components.SectionHeader
import com.clockin.app.ui.components.StatusChip
import com.clockin.app.ui.components.TargetProgressCard
import com.clockin.app.ui.components.TimeDisplay
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.ButtonShape
import com.clockin.app.ui.theme.CyanSecondary
import com.clockin.app.ui.theme.NightBorder
import com.clockin.app.ui.theme.NightSurfaceHigh
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    repository: ClockRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionHeader(
                title = "ShiftLog",
                subtitle = buildString {
                    if (state.isReady) append("班次 ${state.shiftDate} · ")
                    append("本周期 ${state.cycleLabel}")
                },
                icon = AppIcons.Schedule,
            )

            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("今日打卡", style = MaterialTheme.typography.titleLarge)
                    val detail = state.detail
                    if (!state.isReady) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(NightSurfaceHigh),
                        )
                    } else if (detail == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(NightSurfaceHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("尚未打卡", color = TextSecondary)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            TimeDisplay(
                                time = ShiftCalculator.formatTime(detail.record.clockInTime).take(5),
                                label = "上班",
                            )
                            Text("→", style = MaterialTheme.typography.headlineMedium, color = TextSecondary)
                            TimeDisplay(
                                time = ShiftCalculator.formatTime(detail.record.clockOutTime).take(5),
                                label = "下班",
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (detail.record.clockInTime != null) {
                                StatusChip(
                                    ShiftCalculator.statusLabel(detail.clockInStatus),
                                    detail.clockInStatus,
                                )
                            }
                            if (detail.record.clockOutTime != null) {
                                StatusChip(
                                    ShiftCalculator.statusLabel(detail.clockOutStatus),
                                    detail.clockOutStatus,
                                )
                            }
                        }
                        detail.hoursWorked?.let {
                            Text(
                                "工时 ${"%.2f".format(it)} 小时",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = viewModel::clockIn,
                    enabled = state.isReady && state.canClockIn,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = NightSurfaceHigh,
                        disabledContentColor = TextSecondary,
                    ),
                ) {
                    Text("上班打卡", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = viewModel::clockOut,
                    enabled = state.isReady && state.canClockOut,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(
                            1.dp,
                            if (state.isReady && state.canClockOut) CyanSecondary else NightBorder,
                            ButtonShape,
                        ),
                    shape = ButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanSecondary,
                        disabledContentColor = TextSecondary,
                    ),
                ) {
                    Text("下班打卡", style = MaterialTheme.typography.labelLarge)
                }
            }

            state.progress?.let { TargetProgressCard(it) }
        }
    }
}
