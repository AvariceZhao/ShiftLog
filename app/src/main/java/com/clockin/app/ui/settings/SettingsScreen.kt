package com.clockin.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.clockin.app.ui.theme.NightBorder
import com.clockin.app.ui.theme.NightSurfaceHigh
import com.clockin.app.ui.theme.StatusMissed
import com.clockin.app.ui.theme.TextMuted
import com.clockin.app.ui.theme.TextSecondary
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.delay

private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

private data class SettingsForm(
    val clockInText: String,
    val clockOutText: String,
    val isClockOutNextDay: Boolean,
    val cycleStartDayText: String,
    val targetDaysText: String,
    val targetHoursText: String,
    val calendarHoursDecimalPlaces: Int,
    val showExpectedClockOut: Boolean,
    val showElapsedWorkTime: Boolean,
    val showHistoryCycleSummary: Boolean,
) {
    companion object {
        fun from(settings: AppSettings): SettingsForm = SettingsForm(
            clockInText = settings.standardClockIn.format(timeFormatter),
            clockOutText = settings.standardClockOut.format(timeFormatter),
            isClockOutNextDay = settings.isClockOutNextDay,
            cycleStartDayText = settings.cycleStartDay.toString(),
            targetDaysText = settings.targetDays.toString(),
            targetHoursText = settings.targetHours.toInputString(),
            calendarHoursDecimalPlaces = settings.calendarHoursDecimalPlaces,
            showExpectedClockOut = settings.showExpectedClockOut,
            showElapsedWorkTime = settings.showElapsedWorkTime,
            showHistoryCycleSummary = settings.showHistoryCycleSummary,
        )
    }

    fun parseClockIn(): Result<LocalTime> = parseTime(clockInText, "标准上班时间")

    fun parseClockOut(): Result<LocalTime> = parseTime(clockOutText, "标准下班时间")

    fun parseCycleStartDay(): Result<Int> = parseInt(cycleStartDayText, "周期起始日", 1, 28)

    fun parseTargetDays(): Result<Int> = parseInt(targetDaysText, "目标出勤天数", 1, Int.MAX_VALUE)

    fun parseTargetHours(): Result<Float> = parseFloat(targetHoursText, "目标总工时")

    private fun parseTime(raw: String, label: String): Result<LocalTime> {
        val text = raw.trim()
        if (text.isEmpty()) {
            return Result.failure(IllegalArgumentException("$label 不能为空"))
        }
        return try {
            Result.success(LocalTime.parse(text, timeFormatter))
        } catch (_: DateTimeParseException) {
            Result.failure(IllegalArgumentException("$label 请用 H:mm 或 HH:mm，如 7:00"))
        }
    }

    private fun parseInt(raw: String, label: String, min: Int, max: Int): Result<Int> {
        val text = raw.trim()
        if (text.isEmpty()) {
            return Result.failure(IllegalArgumentException("$label 不能为空"))
        }
        val value = text.toIntOrNull()
        if (value == null) {
            return Result.failure(IllegalArgumentException("$label 请输入整数"))
        }
        if (value < min || value > max) {
            return Result.failure(IllegalArgumentException("$label 请在 $min-$max 之间"))
        }
        return Result.success(value)
    }

    private fun parseFloat(raw: String, label: String): Result<Float> {
        val text = raw.trim()
        if (text.isEmpty()) {
            return Result.failure(IllegalArgumentException("$label 不能为空"))
        }
        val value = text.toFloatOrNull()
        if (value == null) {
            return Result.failure(IllegalArgumentException("$label 请输入数字"))
        }
        if (value < 0f) {
            return Result.failure(IllegalArgumentException("$label 不能为负数"))
        }
        return Result.success(value)
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
    var saveNoticeVersion by remember { mutableIntStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }

    fun markSaved() {
        message = "已自动保存"
        isError = false
        saveNoticeVersion++
    }

    LaunchedEffect(saveNoticeVersion) {
        if (saveNoticeVersion == 0 || isError) return@LaunchedEffect
        delay(1_500)
        message = null
    }

    fun <T> saveField(
        value: Result<T>,
        transform: (AppSettings, T) -> AppSettings,
    ) {
        value.onSuccess { fieldValue ->
            viewModel.update { current -> transform(current, fieldValue) }
            markSaved()
        }.onFailure { error ->
            message = error.message ?: "输入有误，请检查后重试"
            isError = true
            saveNoticeVersion++
        }
    }

    ScreenBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onFocusLost = {
                            saveField(form.parseClockIn()) { settings, value ->
                                settings.copy(standardClockIn = value)
                            }
                        },
                    )
                    SettingTextField(
                        label = "标准下班",
                        value = form.clockOutText,
                        onValueChange = { form = form.copy(clockOutText = it) },
                        placeholder = "5:00",
                        onFocusLost = {
                            saveField(form.parseClockOut()) { settings, value ->
                                settings.copy(standardClockOut = value)
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
                            checked = form.isClockOutNextDay,
                            onCheckedChange = { checked ->
                                form = form.copy(isClockOutNextDay = checked)
                                viewModel.update { it.copy(isClockOutNextDay = checked) }
                                markSaved()
                            },
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
                        onFocusLost = {
                            saveField(form.parseCycleStartDay()) { settings, value ->
                                settings.copy(cycleStartDay = value)
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
                    Text("显示", style = MaterialTheme.typography.titleLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("推荐下班时间", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "已上班未下班时，在打卡页显示预期下班时间",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = form.showExpectedClockOut,
                            onCheckedChange = { checked ->
                                form = form.copy(showExpectedClockOut = checked)
                                viewModel.update { it.copy(showExpectedClockOut = checked) }
                                markSaved()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberPrimary,
                                checkedTrackColor = AmberPrimary.copy(alpha = 0.4f),
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("已上班时长", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "已上班未下班时，在打卡页显示累计时长",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = form.showElapsedWorkTime,
                            onCheckedChange = { checked ->
                                form = form.copy(showElapsedWorkTime = checked)
                                viewModel.update { it.copy(showElapsedWorkTime = checked) }
                                markSaved()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberPrimary,
                                checkedTrackColor = AmberPrimary.copy(alpha = 0.4f),
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("历史周期汇总", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "在历史页显示打卡天数和累计工时",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = form.showHistoryCycleSummary,
                            onCheckedChange = { checked ->
                                form = form.copy(showHistoryCycleSummary = checked)
                                viewModel.update { it.copy(showHistoryCycleSummary = checked) }
                                markSaved()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberPrimary,
                                checkedTrackColor = AmberPrimary.copy(alpha = 0.4f),
                            ),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("日历工时小数", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "历史页出勤日历格内工时显示精度",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = form.calendarHoursDecimalPlaces == 1,
                                onClick = {
                                    form = form.copy(calendarHoursDecimalPlaces = 1)
                                    viewModel.update { it.copy(calendarHoursDecimalPlaces = 1) }
                                    markSaved()
                                },
                                label = { Text("1 位") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary.copy(alpha = 0.18f),
                                    selectedLabelColor = AmberPrimary,
                                ),
                            )
                            FilterChip(
                                selected = form.calendarHoursDecimalPlaces == 2,
                                onClick = {
                                    form = form.copy(calendarHoursDecimalPlaces = 2)
                                    viewModel.update { it.copy(calendarHoursDecimalPlaces = 2) }
                                    markSaved()
                                },
                                label = { Text("2 位") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberPrimary.copy(alpha = 0.18f),
                                    selectedLabelColor = AmberPrimary,
                                ),
                            )
                        }
                    }
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
                        onFocusLost = {
                            saveField(form.parseTargetDays()) { settings, value ->
                                settings.copy(targetDays = value)
                            }
                        },
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
                        onFocusLost = {
                            saveField(form.parseTargetHours()) { settings, value ->
                                settings.copy(targetHours = value)
                            }
                        },
                    )
                }
            }

            if (isError) message?.let {
                Text(
                    it,
                    color = StatusMissed,
                )
            }

            Text(
                "ShiftLog · AvariceZhao",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .clickable { showAbout = true },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
            }

            AnimatedVisibility(
                visible = message != null && !isError,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                enter = fadeIn(animationSpec = tween(durationMillis = 160)),
                exit = fadeOut(animationSpec = tween(durationMillis = 220)),
            ) {
                message?.let { notice ->
                    Text(
                        notice,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(NightSurfaceHigh)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                }
            }
        }
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocusLost: () -> Unit = {},
) {
    var wasFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (wasFocused && !focusState.isFocused) onFocusLost()
                wasFocused = focusState.isFocused
            },
        shape = MaterialTheme.shapes.medium,
        colors = fieldColors,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
