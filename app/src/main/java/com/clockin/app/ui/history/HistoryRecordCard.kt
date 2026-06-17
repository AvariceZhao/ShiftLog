package com.clockin.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.clockin.app.domain.PunchStatus
import com.clockin.app.domain.RecordDetail
import com.clockin.app.domain.ShiftCalculator
import com.clockin.app.ui.components.AppCard
import com.clockin.app.ui.components.StatusChip
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun HistoryRecordCard(
    detail: RecordDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier.clickable(onClick = onClick),
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
                            "迟到" -> PunchStatus.LATE
                            "早退" -> PunchStatus.EARLY
                            "缺卡" -> PunchStatus.MISSED_OUT
                            else -> PunchStatus.MISSED_OUT
                        }
                        StatusChip(tag, status)
                    }
                }
            }
        }
    }
}
