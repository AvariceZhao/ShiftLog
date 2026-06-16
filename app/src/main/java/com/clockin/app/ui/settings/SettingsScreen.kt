package com.clockin.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.clockin.app.ui.components.AppIcons
import com.clockin.app.ui.components.AppCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.components.ScreenBackground
import com.clockin.app.ui.components.SectionHeader
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.ButtonShape
import com.clockin.app.ui.theme.NightBorder
import com.clockin.app.ui.theme.NightSurfaceHigh
import com.clockin.app.ui.theme.TextSecondary
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val fieldColors
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = NightBorder,
        focusedContainerColor = NightSurfaceHigh,
        unfocusedContainerColor = NightSurfaceHigh,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

@Composable
fun SettingsScreen(
    repository: ClockRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
    val saved by viewModel.settings.collectAsStateWithLifecycle()
    var draft by remember(saved) { mutableStateOf(saved) }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(
                title = "设置",
                subtitle = "班次、周期与目标",
                icon = AppIcons.Settings,
            )

            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("班次时间", style = MaterialTheme.typography.titleLarge)
                    TimeField(
                        label = "标准上班 (HH:mm)",
                        value = draft.standardClockIn.format(timeFormatter),
                        onValueChange = {
                            runCatching {
                                draft = draft.copy(standardClockIn = LocalTime.parse(it, timeFormatter))
                            }
                        },
                    )
                    TimeField(
                        label = "标准下班 (HH:mm)",
                        value = draft.standardClockOut.format(timeFormatter),
                        onValueChange = {
                            runCatching {
                                draft = draft.copy(standardClockOut = LocalTime.parse(it, timeFormatter))
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("下班跨至次日", style = MaterialTheme.typography.bodyLarge)
                            Text("夜班请保持开启", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = draft.isClockOutNextDay,
                            onCheckedChange = { draft = draft.copy(isClockOutNextDay = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberPrimary,
                                checkedTrackColor = AmberPrimary.copy(alpha = 0.4f),
                            ),
                        )
                    }
                }
            }

            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("工资周期", style = MaterialTheme.typography.titleLarge)
                    IntField(
                        label = "周期起始日 (1-28)",
                        value = draft.cycleStartDay.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.coerceIn(1, 28)?.let { day ->
                                draft = draft.copy(cycleStartDay = day)
                            }
                        },
                    )
                    Text(
                        "例：起始日 9 → 周期为 9 号 ~ 次月 8 号",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            AppCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("目标", style = MaterialTheme.typography.titleLarge)
                    IntField(
                        label = "目标出勤天数",
                        value = draft.targetDays.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.takeIf { v -> v > 0 }?.let { days ->
                                draft = draft.copy(targetDays = days)
                            }
                        },
                    )
                    OutlinedTextField(
                        value = draft.targetHours.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.takeIf { v -> v >= 0 }?.let { hours ->
                                draft = draft.copy(targetHours = hours)
                            }
                        },
                        label = { Text("目标总工时 (小时)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors,
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.save(draft)
                    message = "设置已保存"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
            ) {
                Text("保存设置", style = MaterialTheme.typography.labelLarge)
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TimeField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors,
        singleLine = true,
    )
}

@Composable
private fun IntField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors,
        singleLine = true,
    )
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
