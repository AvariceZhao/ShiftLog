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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.ExpectedClockOutCalculator
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.ClockRecord
import com.clockin.app.ui.components.AppIcons
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.components.FireworksOverlay
import com.clockin.app.ui.components.ScreenBackground
import com.clockin.app.ui.components.SectionHeader
import com.clockin.app.ui.components.StatusChip
import com.clockin.app.ui.components.TargetProgressCard
import com.clockin.app.ui.components.TimeDisplay
import com.clockin.app.ui.history.RecordEditDialog
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.ButtonShape
import com.clockin.app.ui.theme.CyanSecondary
import com.clockin.app.ui.theme.NightBorder
import com.clockin.app.ui.theme.NightSurfaceHigh
import com.clockin.app.ui.theme.TextMuted
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    repository: ClockRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showFireworks by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ClockRecord?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.fireworksEvent.collect {
            showFireworks = true
        }
    }

    ScreenBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        val expectedClockOut = if (settings.showExpectedClockOut) {
                            state.progress?.let { progress ->
                                ExpectedClockOutCalculator.calculate(
                                    clockInMs = detail.record.clockInTime,
                                    clockOutMs = detail.record.clockOutTime,
                                    progress = progress,
                                )
                            }
                        } else {
                            null
                        }
                        val clockOutHints = buildList {
                            expectedClockOut?.byTargetDays?.let { add(it.label to it.timeText) }
                            expectedClockOut?.byCycleDays?.let { add(it.label to it.timeText) }
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    TimeDisplay(
                                        time = ShiftCalculator.formatTime(detail.record.clockInTime).take(5),
                                        label = "上班",
                                        onClick = detail.record.clockInTime?.let { { editing = detail.record } },
                                    )
                                }
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    TimeDisplay(
                                        time = ShiftCalculator.formatTime(detail.record.clockOutTime).take(5),
                                        label = "下班",
                                        hints = clockOutHints,
                                        onClick = detail.record.clockOutTime?.let { { editing = detail.record } },
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.align(Alignment.TopCenter),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "→",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextSecondary,
                                )
                                if (settings.showElapsedWorkTime) {
                                    state.elapsedWorkMillis?.let { elapsed ->
                                        Text(
                                            "已上班",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                        )
                                        Text(
                                            ShiftCalculator.formatElapsedWorkTime(elapsed),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.height(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (settings.showPunchStatus) {
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

            if (settings.showHomeTargetProgress) {
                state.progress?.let { TargetProgressCard(it) }
            }
            }

            if (showFireworks) {
                FireworksOverlay(
                    onFinished = { showFireworks = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f),
                )
            }
        }
    }

    editing?.let { record ->
        RecordEditDialog(
            record = record,
            settings = settings,
            startInExactTimeMode = true,
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
