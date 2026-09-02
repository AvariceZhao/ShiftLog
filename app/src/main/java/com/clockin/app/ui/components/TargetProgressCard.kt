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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clockin.app.domain.StatsCalculator
import com.clockin.app.domain.CycleStats
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
                captionStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            ProgressBar(
                label = "累计工时",
                progress = hoursProgress,
                caption = "${"%.1f".format(progress.totalHours)} / ${progress.targetHours}h",
                captionStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                ),
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatRow(
                    label = "剩余",
                    value = "${StatsCalculator.formatRemainingDays(progress.remainingDays)} · " +
                        StatsCalculator.formatRemainingHours(progress.remainingHours),
                )

                MetricWithFormula(
                    label = "当前日均",
                    formula = "已累计工时 ÷ 已打卡天数",
                    value = progress.currentDailyAvg?.let { "${"%.1f".format(it)}h" } ?: "--",
                    highlight = progress.currentDailyAvg != null,
                )

                MetricWithFormula(
                    label = "所需日均（按目标出勤）",
                    formula = if (progress.daysTargetMetHoursNotMet) {
                        "出勤天数已满，不计算日均"
                    } else {
                        "剩余工时 ÷ 目标剩余出勤天数（${progress.remainingDays.coerceAtLeast(0)} 天）"
                    },
                    value = if (progress.daysTargetMetHoursNotMet) {
                        "还需 ${StatsCalculator.formatRemainingHours(progress.remainingHours)}"
                    } else {
                        StatsCalculator.formatDailyAvg(
                            progress.requiredDailyByTargetDays,
                            progress.requiredDailyByTargetDaysUnreachable,
                        )
                    },
                    highlight = progress.requiredDailyByTargetDays != null,
                )

                MetricWithFormula(
                    label = "所需日均（按周期日历）",
                    formula = "剩余工时 ÷ 周期剩余日历天数（${progress.cycleDaysRemaining} 天）",
                    value = StatsCalculator.formatDailyAvg(
                        progress.requiredDailyByCycleDays,
                        progress.requiredDailyByCycleDaysUnreachable,
                    ),
                    highlight = progress.requiredDailyByCycleDays != null,
                )
            }
        }
    }
}

@Composable
fun CycleSummaryCard(stats: CycleStats, modifier: Modifier = Modifier) {
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
                    AppIcons.Calendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("周期汇总", style = MaterialTheme.typography.titleLarge)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryMetric(
                    label = "打卡天数",
                    value = "${stats.clockedDays} 天",
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "累计工时",
                    value = "${"%.1f".format(stats.totalHours)} h",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
        )
    }
}

@Composable
private fun MetricWithFormula(
    label: String,
    formula: String,
    value: String,
    highlight: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        StatRow(label = label, value = value, highlight = highlight)
        Text(
            formula,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
