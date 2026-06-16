package com.clockin.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clockin.app.domain.StatsCalculator
import com.clockin.app.domain.TargetProgress
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun TargetProgressCard(progress: TargetProgress, modifier: Modifier = Modifier) {
    val daysProgress = if (progress.targetDays > 0) {
        progress.clockedDays.toFloat() / progress.targetDays
    } else {
        0f
    }
    val hoursProgress = if (progress.targetHours > 0) {
        progress.totalHours.toFloat() / progress.targetHours
    } else {
        0f
    }

    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    AppIcons.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("目标进度", style = MaterialTheme.typography.titleLarge)
            }

            ProgressBar(
                label = "出勤天数",
                progress = daysProgress,
                caption = "${progress.clockedDays} / ${progress.targetDays} 天",
            )
            ProgressBar(
                label = "累计工时",
                progress = hoursProgress,
                caption = "${"%.1f".format(progress.totalHours)} / ${progress.targetHours}h",
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow(
                    label = "剩余",
                    value = "${StatsCalculator.formatRemainingDays(progress.remainingDays)} · " +
                        StatsCalculator.formatRemainingHours(progress.remainingHours),
                )
                StatRow(
                    label = "当前日均",
                    value = progress.currentDailyAvg?.let { "${"%.1f".format(it)}h" } ?: "--",
                    highlight = true,
                )
                StatRow(
                    label = "接下来所需日均",
                    value = when {
                        progress.requiredDailyAvg == null -> "--"
                        progress.requiredDailyUnreachable ->
                            "${"%.1f".format(progress.requiredDailyAvg)}h · 无法达成"
                        else -> "${"%.1f".format(progress.requiredDailyAvg)}h"
                    },
                    highlight = progress.requiredDailyAvg != null,
                )
            }

            Text(
                "周期剩余 ${progress.cycleDaysRemaining} 天",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}
