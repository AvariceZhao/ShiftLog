package com.clockin.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.domain.toShiftDateString
import com.clockin.app.ui.components.AppIcons
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.NightBorder
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.NightSurfaceHigh
import com.clockin.app.ui.theme.StatusMissed
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private enum class EditMode { Duration, ExactTime }

private enum class TimeFieldTarget {
    DurationIn,
    DurationOut,
    ExactIn,
    ExactOut,
}

private const val DEFAULT_DURATION_HOURS = "8"

private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")
private val shiftDateDisplayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd (E)", Locale.CHINA)
private val zoneId = ZoneId.systemDefault()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditDialog(
    record: ClockRecord,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (ClockRecord) -> Unit,
    onDelete: () -> Unit,
) {
    var mode by remember(record) { mutableStateOf(EditMode.Duration) }
    var shiftDate by remember(record.shiftDate) { mutableStateOf(record.shiftDate) }
    var durationText by remember(record) {
        mutableStateOf(record.toDurationText().ifEmpty { DEFAULT_DURATION_HOURS })
    }
    var optionalClockInText by remember(record) {
        mutableStateOf(record.clockInTime?.toLocalTimeText() ?: "")
    }
    var optionalClockOutText by remember(record) {
        mutableStateOf(record.clockOutTime?.toLocalTimeText() ?: "")
    }
    var exactClockInText by remember(record) {
        mutableStateOf(record.clockInTime?.toLocalTimeText() ?: "")
    }
    var exactClockOutText by remember(record) {
        mutableStateOf(record.clockOutTime?.toLocalTimeText() ?: "")
    }
    var durationOutNextDay by remember(record, settings) {
        mutableStateOf(record.inferClockOutNextDay(record.shiftDate) ?: settings.isClockOutNextDay)
    }
    var exactOutNextDay by remember(record, settings) {
        mutableStateOf(record.inferClockOutNextDay(record.shiftDate) ?: settings.isClockOutNextDay)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var activeTimeField by remember { mutableStateOf<TimeFieldTarget?>(null) }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == EditMode.Duration,
                        onClick = { mode = EditMode.Duration; error = null },
                        label = { Text("按时长") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AmberPrimary,
                        ),
                    )
                    FilterChip(
                        selected = mode == EditMode.ExactTime,
                        onClick = { mode = EditMode.ExactTime; error = null },
                        label = { Text("按时刻") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AmberPrimary,
                        ),
                    )
                }

                ShiftDateField(
                    shiftDate = shiftDate,
                    fieldColors = fieldColors,
                    onClick = { showDatePicker = true },
                )

                when (mode) {
                    EditMode.Duration -> {
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { text ->
                                durationText = text.filter { it.isDigit() || it == '.' }
                            },
                            label = { Text("工时 (小时)") },
                            placeholder = { Text(DEFAULT_DURATION_HOURS) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        TimePickerField(
                            label = "上班时间 (可选)",
                            value = optionalClockInText,
                            placeholder = "默认 ${settings.standardClockIn.format(timeFormatter)}",
                            fieldColors = fieldColors,
                            onClick = { activeTimeField = TimeFieldTarget.DurationIn },
                        )
                        TimePickerField(
                            label = "下班时间 (可选)",
                            value = optionalClockOutText.formatOutDisplay(durationOutNextDay),
                            placeholder = "留空则按工时推算",
                            fieldColors = fieldColors,
                            onClick = { activeTimeField = TimeFieldTarget.DurationOut },
                        )
                        Text(
                            "日期点日历选；时间点时钟选。下班可勾选「次日」",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    EditMode.ExactTime -> {
                        TimePickerField(
                            label = "上班时间",
                            value = exactClockInText,
                            placeholder = "点击选择",
                            fieldColors = fieldColors,
                            onClick = { activeTimeField = TimeFieldTarget.ExactIn },
                        )
                        TimePickerField(
                            label = "下班时间",
                            value = exactClockOutText.formatOutDisplay(exactOutNextDay),
                            placeholder = "点击选择",
                            fieldColors = fieldColors,
                            onClick = { activeTimeField = TimeFieldTarget.ExactOut },
                        )
                        Text(
                            "上班在班次日当天；下班可选「次日」（如次日 5:00）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                error?.let {
                    Text(it, color = StatusMissed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = null
                    runCatching {
                        val saved = when (mode) {
                            EditMode.Duration -> buildFromDuration(
                                shiftDate = shiftDate,
                                durationText = durationText,
                                optionalClockInText = optionalClockInText,
                                optionalClockOutText = optionalClockOutText,
                                clockOutNextDay = durationOutNextDay,
                                settings = settings,
                            )
                            EditMode.ExactTime -> buildFromExactTimes(
                                shiftDate = shiftDate,
                                clockInText = exactClockInText,
                                clockOutText = exactClockOutText,
                                clockOutNextDay = exactOutNextDay,
                            )
                        }
                        onSave(saved)
                    }.onFailure { e ->
                        error = e.message ?: "保存失败，请检查输入"
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = shiftDate.toDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            shiftDate = millis.fromDatePickerMillis().toShiftDateString()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("确定", color = AmberPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    activeTimeField?.let { target ->
        val isClockOutTarget = target == TimeFieldTarget.DurationOut || target == TimeFieldTarget.ExactOut
        val initialTime = when (target) {
            TimeFieldTarget.DurationIn ->
                optionalClockInText.toLocalTimeOr(settings.standardClockIn)
            TimeFieldTarget.DurationOut ->
                optionalClockOutText.toLocalTimeOr(settings.standardClockOut)
            TimeFieldTarget.ExactIn ->
                exactClockInText.toLocalTimeOr(settings.standardClockIn)
            TimeFieldTarget.ExactOut ->
                exactClockOutText.toLocalTimeOr(settings.standardClockOut)
        }
        var pickerOutNextDay by remember(target) {
            mutableStateOf(
                when (target) {
                    TimeFieldTarget.DurationOut -> durationOutNextDay
                    TimeFieldTarget.ExactOut -> exactOutNextDay
                    else -> settings.isClockOutNextDay
                },
            )
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { activeTimeField = null },
            containerColor = NightSurface,
            title = { Text(if (isClockOutTarget) "选择下班时间" else "选择上班时间") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimePicker(state = timePickerState)
                    if (isClockOutTarget) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("次日下班", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "如 ${shiftDate.toDisplayDate()} 上班 → 次日 ${settings.standardClockOut.format(timeFormatter)} 下班",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = pickerOutNextDay,
                                onCheckedChange = { pickerOutNextDay = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmberPrimary,
                                    checkedTrackColor = AmberPrimary.copy(alpha = 0.4f),
                                ),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formatted = LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute,
                        ).format(timeFormatter)
                        when (target) {
                            TimeFieldTarget.DurationIn -> optionalClockInText = formatted
                            TimeFieldTarget.DurationOut -> {
                                optionalClockOutText = formatted
                                durationOutNextDay = pickerOutNextDay
                            }
                            TimeFieldTarget.ExactIn -> exactClockInText = formatted
                            TimeFieldTarget.ExactOut -> {
                                exactClockOutText = formatted
                                exactOutNextDay = pickerOutNextDay
                            }
                        }
                        activeTimeField = null
                    },
                ) {
                    Text("确定", color = AmberPrimary)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            when (target) {
                                TimeFieldTarget.DurationIn -> optionalClockInText = ""
                                TimeFieldTarget.DurationOut -> {
                                    optionalClockOutText = ""
                                    durationOutNextDay = settings.isClockOutNextDay
                                }
                                TimeFieldTarget.ExactIn -> exactClockInText = ""
                                TimeFieldTarget.ExactOut -> {
                                    exactClockOutText = ""
                                    exactOutNextDay = settings.isClockOutNextDay
                                }
                            }
                            activeTimeField = null
                        },
                    ) {
                        Text("清除", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { activeTimeField = null }) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
    }
}

@Composable
private fun ShiftDateField(
    shiftDate: String,
    fieldColors: TextFieldColors,
    onClick: () -> Unit,
) {
    OutlinedTextField(
        value = shiftDate.toDisplayDate(),
        onValueChange = {},
        readOnly = true,
        label = { Text("班次日期") },
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(AppIcons.Calendar, contentDescription = "选择日期", tint = AmberPrimary)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        colors = fieldColors,
        singleLine = true,
    )
}

@Composable
private fun TimePickerField(
    label: String,
    value: String,
    placeholder: String,
    fieldColors: TextFieldColors,
    onClick: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(AppIcons.Schedule, contentDescription = "选择时间", tint = AmberPrimary)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        colors = fieldColors,
        singleLine = true,
    )
}

fun newRecordForToday(): ClockRecord =
    ClockRecord(LocalDate.now().toShiftDateString(), null, null)

private fun buildFromDuration(
    shiftDate: String,
    durationText: String,
    optionalClockInText: String,
    optionalClockOutText: String,
    clockOutNextDay: Boolean,
    settings: AppSettings,
): ClockRecord {
    val date = parseShiftDate(shiftDate)
    val clockInTime = optionalClockInText.trim().takeIf { it.isNotEmpty() }
        ?.let { parseClockInTime(it) }
        ?: settings.standardClockIn
    val clockIn = LocalDateTime.of(date, clockInTime)
    val inMs = clockIn.atZone(zoneId).toInstant().toEpochMilli()

    val outMs = optionalClockOutText.trim().takeIf { it.isNotEmpty() }?.let { text ->
        resolveClockOut(text, date, clockIn, clockOutNextDay)
    } ?: run {
        val hours = durationText.trim().toFloatOrNull()
            ?: throw IllegalArgumentException("请输入有效工时，或选择下班时间")
        if (hours <= 0f) throw IllegalArgumentException("工时必须大于 0")
        inMs + (hours * 3_600_000f).toLong()
    }

    return ClockRecord(date.toShiftDateString(), inMs, outMs)
}

private fun buildFromExactTimes(
    shiftDate: String,
    clockInText: String,
    clockOutText: String,
    clockOutNextDay: Boolean,
): ClockRecord {
    val date = parseShiftDate(shiftDate)
    val inMs = clockInText.trim().takeIf { it.isNotEmpty() }?.let { text ->
        LocalDateTime.of(date, parseClockInTime(text))
            .atZone(zoneId).toInstant().toEpochMilli()
    }
    val outMs = clockOutText.trim().takeIf { it.isNotEmpty() }?.let { text ->
        val clockIn = inMs?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zoneId)
        } ?: LocalDateTime.of(date, LocalTime.MIDNIGHT)
        resolveClockOut(text, date, clockIn, clockOutNextDay)
    }
    if (inMs == null && outMs == null) {
        throw IllegalArgumentException("请至少选择上班或下班时间")
    }
    return ClockRecord(date.toShiftDateString(), inMs, outMs)
}

private fun resolveClockOut(
    outText: String,
    shiftDate: LocalDate,
    clockIn: LocalDateTime,
    isNextDay: Boolean,
): Long {
    val outTime = parseClockInTime(outText)
    val outDate = if (isNextDay) shiftDate.plusDays(1) else shiftDate
    val outDt = LocalDateTime.of(outDate, outTime)
    if (!outDt.isAfter(clockIn)) {
        throw IllegalArgumentException("下班时间须晚于上班时间")
    }
    return outDt.atZone(zoneId).toInstant().toEpochMilli()
}

private fun String.formatOutDisplay(isNextDay: Boolean): String {
    if (isEmpty()) return ""
    return if (isNextDay) "次日 $this" else this
}

private fun ClockRecord.inferClockOutNextDay(shiftDate: String): Boolean? {
    val outMs = clockOutTime ?: return null
    val shift = LocalDate.parse(shiftDate, DateTimeFormatter.ISO_LOCAL_DATE)
    val outDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(outMs), zoneId).toLocalDate()
    return outDate.isAfter(shift)
}

private fun parseClockInTime(text: String): LocalTime = try {
    LocalTime.parse(text.trim(), timeFormatter)
} catch (_: DateTimeParseException) {
    throw IllegalArgumentException("时间格式无效")
}

private fun String.toLocalTimeOr(default: LocalTime): LocalTime =
    trim().takeIf { it.isNotEmpty() }?.let { parseClockInTime(it) } ?: default

private fun parseShiftDate(text: String): LocalDate = try {
    LocalDate.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
} catch (_: DateTimeParseException) {
    throw IllegalArgumentException("请选择班次日期")
}

private fun String.toDisplayDate(): String = try {
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE).format(shiftDateDisplayFormatter)
} catch (_: DateTimeParseException) {
    this
}

private fun String.toDatePickerMillis(): Long =
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE).toDatePickerMillis()

private fun LocalDate.toDatePickerMillis(): Long =
    atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

private fun Long.fromDatePickerMillis(): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private fun ClockRecord.toDurationText(): String {
    if (clockInTime == null || clockOutTime == null) return ""
    val hours = ShiftCalculator.hoursWorked(clockInTime, clockOutTime)
    val whole = hours.toLong()
    return if (hours == whole.toDouble()) whole.toString() else "%.2f".format(hours)
}

private fun Long.toLocalTimeText(): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zoneId).format(timeFormatter)
