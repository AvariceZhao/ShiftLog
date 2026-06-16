package com.clockin.app.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.clockin.app.ClockInApplication
import com.clockin.app.ui.theme.ClockInTheme
import com.clockin.app.ui.theme.NightSurface
import com.clockin.app.ui.theme.TextSecondary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmPunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val punchType = intent.getStringExtra(EXTRA_PUNCH_TYPE)
        if (punchType !in setOf(PUNCH_IN, PUNCH_OUT)) {
            finish()
            return
        }

        val app = application as ClockInApplication

        lifecycleScope.launch {
            val repository = app.repository
            val (shiftDate, record) = repository.loadActiveShift()
            val allowed = when (punchType) {
                PUNCH_IN -> record?.clockInTime == null
                PUNCH_OUT -> record?.clockInTime != null
                else -> false
            }
            if (!allowed) {
                app.refreshWidgets(
                    intent.getIntExtra(
                        EXTRA_APP_WIDGET_ID,
                        android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID,
                    ),
                )
                finish()
                return@launch
            }

            val shiftDateLabel = shiftDate.substring(5).replace("-", "/")
            val timeSnapshot = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA),
            )
            val isClockIn = punchType == PUNCH_IN

            setContent {
                ClockInTheme {
                    val title = remember { if (isClockIn) "确认上班打卡？" else "确认下班打卡？" }
                    val message = remember { "班次 $shiftDateLabel\n将记录当前时间 $timeSnapshot" }
                    AlertDialog(
                        onDismissRequest = { finish() },
                        containerColor = NightSurface,
                        title = {
                            Text(title, style = MaterialTheme.typography.titleLarge)
                        },
                        text = {
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    lifecycleScope.launch {
                                        if (isClockIn) {
                                            repository.performClockIn()
                                        } else {
                                            repository.performClockOut()
                                        }
                                        val widgetId = intent.getIntExtra(
                                            EXTRA_APP_WIDGET_ID,
                                            android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID,
                                        )
                                        app.refreshWidgets(widgetId)
                                        withContext(Dispatchers.Main.immediate) { finish() }
                                    }
                                },
                            ) {
                                Text("确认", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) {
                                Text("取消", color = TextSecondary)
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PUNCH_TYPE = "punch_type"
        const val EXTRA_APP_WIDGET_ID = "app_widget_id"
        const val PUNCH_IN = "clock_in"
        const val PUNCH_OUT = "clock_out"

        fun intent(context: Context, punchType: String, appWidgetId: Int): Intent =
            Intent(context, ConfirmPunchActivity::class.java).apply {
                putExtra(EXTRA_PUNCH_TYPE, punchType)
                putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }
}
