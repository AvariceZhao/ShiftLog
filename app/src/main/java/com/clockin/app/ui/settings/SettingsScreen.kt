package com.clockin.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockin.app.data.ClockRepository
import com.clockin.app.domain.AppSettings
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.components.AppIcons
import com.clockin.app.ui.components.ScreenBackground
import com.clockin.app.ui.components.SectionHeader
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.ButtonShape
import com.clockin.app.ui.theme.NightBorder
import com.clockin.app.ui.theme.NightSurfaceHigh
import com.clockin.app.ui.theme.StatusMissed
import com.clockin.app.ui.theme.TextSecondary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

private data class SettingsForm(
    val clockInText: String,
    val clockOutText: String,
    val isClockOutNextDay: Boolean,
    val cycleStartDayText: String,
    val targetDaysText: String,
    val targetHoursText: String,
) {
    companion object {
        fun from(settings: AppSettings): SettingsForm = SettingsForm(
            clockInText = settings.standardClockIn.format(timeFormatter),
            clockOutText = settings.standardClockOut.format(timeFormatter),
            isClockOutNextDay = settings.isClockOutNextDay,
            cycleStartDayText = settings.cycleStartDay.toString(),
            targetDaysText = settings.targetDays.toString(),
            targetHoursText = settings.targetHours.toInputString(),
        )
    }

    fun toAppSettings(): Result<AppSettings> {
        val clockIn = parseTime(clockInText.trim(), "标准上班时间") ?: return parseError()
        val clockOut = parseTime(clockOutText.trim(), "标准下班时间") ?: return parseError()
        val cycleStartDay = parseInt(cycleStartDayText.trim(), "周期起始日", 1, 28) ?: return parseError()
        val targetDays = parseInt(targetDaysText.trim(), "目标出勤天数", 1, Int.MAX_VALUE) ?: return parseError()
        val targetHours = parseFloat(targetHoursText.trim(), "目标总工时") ?: return parseError()
        return Result.success(
            AppSettings(
                standardClockIn = clockIn,
                standardClockOut = clockOut,
                isClockOutNextDay = isClockOutNextDay,
                cycleStartDay = cycleStartDay,
                targetDays = targetDays,
                targetHours = targetHours,
            ),
        )
    }

    private var lastError: String? = null

    private fun parseError(): Result<AppSettings> =
        Result.failure(IllegalArgumentException(lastError ?: "输入格式错误"))

    private fun parseTime(text: String, label: String): LocalTime? {
        if (text.isEmpty()) {
            lastError = "$label 不能为空"
            return null
        }
        return try {
            LocalTime.parse(text, timeFormatter)
        } catch (_: DateTimeParseException) {
            lastError = "$label 请用 H:mm 或 HH:mm，如 7:00"
            null
        }
    }

    private fun parseInt(text: String, label: String, min: Int, max: Int): Int? {
        if (text.isEmpty()) {
            lastError = "$label 不能为空"
            return null
        }
        val value = text.toIntOrNull()
        if (value == null) {
            lastError = "$label 请输入整数"
            return null
        }
        if (value < min || value > max) {
            lastError = "$label 请在 $min-$max 之间"
            return null
        }
        return value
    }

    private fun parseFloat(text: String, label: String): Float? {
        if (text.isEmpty()) {
            lastError = "$label 不能为空"
            return null
        }
        val value = text.toFloatOrNull()
        if (value == null) {
            lastError = "$label 请输入数字"
            return null
        }
        if (value < 0f) {
            lastError = "$label 不能为负数"
            return null
        }
        return value
    }
}

private fun Float.toInputString(): String {
    val whole = toLong()
    return if (this == whole.toFloat()) whole.toString() else toString()
}

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
    var form by remember(saved) { mutableStateOf(SettingsForm.from(saved)) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

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
                    SettingTextField(
                        label = "标准上班",
                        value = form.clockInText,
                        onValueChange = { form = form.copy(clockInText = it) },
                        placeholder = "7:00",
                    )
                    SettingTextField(
                        label = "标准下班",
                        value = form.clockOutText,
                        onValueChange = { form = form.copy(clockOutText = it) },
                        placeholder = "5:00",
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
                            checked = form.isClockOutNextDay,
                            onCheckedChange = { form = form.copy(isClockOutNextDay = it) },
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
                    SettingTextField(
                        label = "周期起始日",
                        value = form.cycleStartDayText,
                        onValueChange = { form = form.copy(cycleStartDayText = it.filter(Char::isDigit)) },
                        placeholder = "9",
                        keyboardType = KeyboardType.Number,
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
                    SettingTextField(
                        label = "目标出勤天数",
                        value = form.targetDaysText,
                        onValueChange = { form = form.copy(targetDaysText = it.filter(Char::isDigit)) },
                        placeholder = "21",
                        keyboardType = KeyboardType.Number,
                    )
                    SettingTextField(
                        label = "目标总工时 (小时)",
                        value = form.targetHoursText,
                        onValueChange = { text ->
                            form = form.copy(
                                targetHoursText = text.filter { it.isDigit() || it == '.' },
                            )
                        },
                        placeholder = "160",
                        keyboardType = KeyboardType.Decimal,
                    )
                }
            }

            Button(
                onClick = {
                    form.toAppSettings()
                        .onSuccess { settings ->
                            viewModel.save(settings)
                            message = "设置已保存"
                            isError = false
                        }
                        .onFailure { error ->
                            message = error.message ?: "输入有误，请检查后重试"
                            isError = true
                        }
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
                Text(
                    it,
                    color = if (isError) StatusMissed else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
