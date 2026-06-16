package com.clockin.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.clockin.app.R

object WidgetColors {
    val background = ColorProvider(R.color.widget_background)
    val textPrimary = ColorProvider(R.color.widget_text_primary)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val amber = ColorProvider(R.color.widget_amber)
    val cyan = ColorProvider(R.color.widget_cyan)
    val onDark = ColorProvider(R.color.widget_background)
    val statusDone = ColorProvider(R.color.widget_status_done)
    val chipDone = ColorProvider(R.color.widget_text_secondary)
}

object WidgetTextStyles {
    val title = TextStyle(
        color = WidgetColors.textPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
    val body = TextStyle(
        color = WidgetColors.textPrimary,
        fontSize = 11.sp,
    )
    val caption = TextStyle(
        color = WidgetColors.textSecondary,
        fontSize = 10.sp,
    )
    val footnote = TextStyle(
        color = WidgetColors.textSecondary,
        fontSize = 9.sp,
    )
    val chip = TextStyle(
        color = WidgetColors.onDark,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
    val statusDone = TextStyle(
        color = WidgetColors.statusDone,
        fontSize = 11.sp,
    )
}

enum class WidgetLayoutStyle {
    COMPACT,
    // TALL,  // 2×3 · QuickPunchWidget
    // WIDE,  // 3×2 · TodayStatusWidget
}

enum class WidgetFootnoteMode {
    /** 剩 X天 · Yh */
    REMAINING,
    // /** X/Y天 · 总工时 · 日均 */
    // PROGRESS,
    // /** 剩余 + 进度两行（高组件） */
    // FULL,
}

@Composable
fun WidgetScaffold(content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.background)
            .padding(6.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
fun WidgetStandardContent(state: WidgetUiState, style: WidgetLayoutStyle) {
    when (style) {
        WidgetLayoutStyle.COMPACT -> WidgetCompactContent(state)
    }
}

/*
// 3×2 宽组件
@Composable
private fun WidgetWideContent(state: WidgetUiState) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
    ) {
        WidgetHeader(state)
        Spacer(modifier = GlanceModifier.height(3.dp))
        WidgetTodayStatusCompact(state)
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetPunchButtonsHorizontal(state, buttonHeight = 36.dp)
        Spacer(modifier = GlanceModifier.height(4.dp))
        WidgetRemainingFootnote(state, WidgetFootnoteMode.PROGRESS)
    }
}
*/

@Composable
private fun WidgetCompactContent(state: WidgetUiState) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
    ) {
        WidgetHeader(state)
        Spacer(modifier = GlanceModifier.height(2.dp))
        WidgetTodayStatusCompact(state)
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetPunchButtonsVertical(
            state = state,
            buttonHeight = 34.dp,
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetRemainingFootnote(state, WidgetFootnoteMode.REMAINING)
    }
}

/*
// 2×3 高组件
@Composable
private fun WidgetTallContent(state: WidgetUiState) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
    ) {
        WidgetHeader(state)
        Spacer(modifier = GlanceModifier.height(4.dp))
        WidgetTodayStatusCompact(state)
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetPunchButtonsVertical(state, buttonHeight = 46.dp)
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetRemainingFootnote(state, WidgetFootnoteMode.FULL)
    }
}
*/

@Composable
fun WidgetHeader(state: WidgetUiState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "ShiftLog",
            style = WidgetTextStyles.title,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = WidgetStateLoader.cycleSummary(state),
            style = WidgetTextStyles.caption,
        )
    }
}

@Composable
fun WidgetTodayStatusCompact(state: WidgetUiState) {
    Text(
        text = WidgetStateLoader.todayCompactLine(state),
        style = if (state.hasClockedIn) WidgetTextStyles.statusDone else WidgetTextStyles.body,
        modifier = GlanceModifier.fillMaxWidth(),
    )
}

@Composable
fun WidgetRemainingFootnote(state: WidgetUiState, mode: WidgetFootnoteMode) {
    when (mode) {
        WidgetFootnoteMode.REMAINING -> {
            Text(
                text = WidgetStateLoader.remainingFootnote(state),
                style = WidgetTextStyles.footnote,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = WidgetStateLoader.requiredDailyFootnote(state),
                style = WidgetTextStyles.footnote,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
        /*
        WidgetFootnoteMode.PROGRESS -> {
            Text(
                text = WidgetStateLoader.progressFootnote(state),
                style = WidgetTextStyles.footnote,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
        WidgetFootnoteMode.FULL -> {
            Text(
                text = WidgetStateLoader.remainingFootnote(state),
                style = WidgetTextStyles.footnote,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = WidgetStateLoader.progressFootnote(state),
                style = WidgetTextStyles.footnote,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
        */
    }
}

@Composable
fun WidgetPunchButtonsVertical(
    state: WidgetUiState,
    buttonHeight: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier) {
        WidgetClockInSlot(
            state = state,
            modifier = GlanceModifier.fillMaxWidth(),
            height = buttonHeight,
        )
        Spacer(modifier = GlanceModifier.height(5.dp))
        WidgetClockOutSlot(
            state = state,
            modifier = GlanceModifier.fillMaxWidth(),
            height = buttonHeight,
        )
    }
}

/*
// 3×2 宽组件 · 横向并排按钮
@Composable
fun WidgetPunchButtonsHorizontal(state: WidgetUiState, buttonHeight: Dp) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetClockInSlot(
            state = state,
            modifier = GlanceModifier.defaultWeight(),
            height = buttonHeight,
        )
        Spacer(modifier = GlanceModifier.padding(horizontal = 6.dp))
        WidgetClockOutSlot(
            state = state,
            modifier = GlanceModifier.defaultWeight(),
            height = buttonHeight,
        )
    }
}
*/

@Composable
private fun WidgetClockInSlot(
    state: WidgetUiState,
    modifier: GlanceModifier,
    height: Dp,
) {
    if (state.canClockIn) {
        val context = LocalContext.current
        Button(
            text = "上班",
            onClick = actionStartActivity(
                ConfirmPunchActivity.intent(
                    context,
                    ConfirmPunchActivity.PUNCH_IN,
                    state.appWidgetId,
                ),
            ),
            modifier = modifier.fillMaxWidth().height(height),
            colors = punchButtonColors(isPrimary = true),
        )
    } else {
        WidgetStatusButton(
            text = "已上班",
            modifier = modifier,
            height = height,
        )
    }
}

@Composable
private fun WidgetClockOutSlot(
    state: WidgetUiState,
    modifier: GlanceModifier,
    height: Dp,
) {
    if (!state.canClockOut) {
        WidgetStatusButton(
            text = "未上班",
            modifier = modifier,
            height = height,
        )
        return
    }
    val context = LocalContext.current
    Button(
        text = "下班",
        onClick = actionStartActivity(
            ConfirmPunchActivity.intent(
                context,
                ConfirmPunchActivity.PUNCH_OUT,
                state.appWidgetId,
            ),
        ),
        modifier = modifier.fillMaxWidth().height(height),
        colors = punchButtonColors(isPrimary = false),
    )
}

@Composable
private fun WidgetStatusButton(
    text: String,
    modifier: GlanceModifier,
    height: Dp,
) {
    Button(
        text = text,
        onClick = actionRunCallback<WidgetNoOpAction>(),
        enabled = false,
        modifier = modifier.fillMaxWidth().height(height),
        colors = statusButtonColors(),
    )
}

@Composable
private fun statusButtonColors() =
    ButtonDefaults.buttonColors(
        backgroundColor = WidgetColors.chipDone,
        contentColor = WidgetColors.onDark,
    )

@Composable
private fun punchButtonColors(isPrimary: Boolean) =
    ButtonDefaults.buttonColors(
        backgroundColor = if (isPrimary) WidgetColors.amber else WidgetColors.cyan,
        contentColor = WidgetColors.onDark,
    )
