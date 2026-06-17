package com.clockin.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clockin.app.domain.AppSettings
import com.clockin.app.domain.ClockRecord
import com.clockin.app.domain.CycleDayStatus
import com.clockin.app.domain.CycleDayStatusCalculator
import com.clockin.app.domain.PayCycle
import com.clockin.app.domain.toShiftDateString
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.theme.NightBorder
import androidx.compose.foundation.layout.size
import com.clockin.app.ui.theme.StatusLate
import com.clockin.app.ui.theme.StatusMissed
import com.clockin.app.ui.theme.StatusNormal
import com.clockin.app.ui.theme.TextMuted
import com.clockin.app.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun CycleCalendarCard(
    cycle: PayCycle,
    records: List<ClockRecord>,
    settings: AppSettings,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recordByDate = records.associateBy { it.shiftDate }
    val gridStart = cycle.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val gridEnd = cycle.end.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("出勤日历", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                }
            }

            var monthCursor: LocalDate? = null
            var date = gridStart
            val rows = mutableListOf<List<LocalDate>>()
            var currentRow = mutableListOf<LocalDate>()
            while (!date.isAfter(gridEnd)) {
                if (date.dayOfWeek == DayOfWeek.MONDAY && currentRow.isNotEmpty()) {
                    rows += currentRow
                    currentRow = mutableListOf()
                }
                currentRow += date
                date = date.plusDays(1)
            }
            if (currentRow.isNotEmpty()) rows += currentRow

            rows.forEach { week ->
                val first = week.first()
                if (monthCursor == null || monthCursor!!.month != first.month || monthCursor!!.year != first.year) {
                    monthCursor = first
                    if (week != rows.first()) {
                        Text(
                            first.format(monthFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    } else {
                        Text(
                            first.format(monthFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { day ->
                        val inCycle = cycle.contains(day)
                        val shiftDate = day.toShiftDateString()
                        val status = if (inCycle) {
                            CycleDayStatusCalculator.statusForDate(
                                date = day,
                                cycle = cycle,
                                record = recordByDate[shiftDate],
                                settings = settings,
                            )
                        } else {
                            CycleDayStatus.OUT_OF_CYCLE
                        }
                        CalendarDayCell(
                            day = day.dayOfMonth,
                            status = status,
                            enabled = inCycle && status != CycleDayStatus.OUT_OF_CYCLE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (inCycle) onDayClick(day)
                            },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(color = StatusNormal.copy(alpha = 0.85f), label = "正常")
                LegendDot(color = StatusLate.copy(alpha = 0.85f), label = "异常")
                LegendDot(color = StatusMissed.copy(alpha = 0.55f), label = "缺勤")
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    status: CycleDayStatus,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when (status) {
        CycleDayStatus.NORMAL -> StatusNormal.copy(alpha = 0.22f)
        CycleDayStatus.ISSUE -> StatusLate.copy(alpha = 0.22f)
        CycleDayStatus.ABSENT -> StatusMissed.copy(alpha = 0.18f)
        CycleDayStatus.PENDING -> Color.Transparent
        CycleDayStatus.OUT_OF_CYCLE -> Color.Transparent
    }
    val borderColor = when {
        !enabled -> Color.Transparent
        status == CycleDayStatus.PENDING -> NightBorder.copy(alpha = 0.35f)
        else -> bg.copy(alpha = 0.9f)
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else TextMuted.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

fun recordForDay(day: LocalDate, records: List<ClockRecord>): ClockRecord? =
    records.firstOrNull { it.shiftDate == day.toShiftDateString() }

fun recordOrPlaceholder(day: LocalDate, records: List<ClockRecord>): ClockRecord =
    recordForDay(day, records) ?: ClockRecord(
        shiftDate = day.toShiftDateString(),
        clockInTime = null,
        clockOutTime = null,
    )
