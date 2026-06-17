package com.clockin.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.clockin.app.widget.ConfirmPunchActivity
import kotlinx.coroutines.launch

/** 桌面快捷方式：直接上下班打卡 */
class ShortcutPunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val punchType = intent.getStringExtra(ConfirmPunchActivity.EXTRA_PUNCH_TYPE)
        if (punchType !in setOf(ConfirmPunchActivity.PUNCH_IN, ConfirmPunchActivity.PUNCH_OUT)) {
            finish()
            return
        }
        val app = application as ClockInApplication
        lifecycleScope.launch {
            val repository = app.repository
            val message = when (punchType) {
                ConfirmPunchActivity.PUNCH_IN -> {
                    val before = repository.loadActiveShift()
                    repository.performClockIn()
                    val after = repository.loadActiveShift()
                    if (after.second?.clockInTime != null && before.second?.clockInTime == null) {
                        "已上班打卡"
                    } else {
                        "今日已上班，无需重复打卡"
                    }
                }
                ConfirmPunchActivity.PUNCH_OUT -> {
                    val before = repository.loadActiveShift()
                    repository.performClockOut()
                    val after = repository.loadActiveShift()
                    if (after.second?.clockOutTime != before.second?.clockOutTime &&
                        after.second?.clockOutTime != null
                    ) {
                        "已下班打卡"
                    } else {
                        "请先上班打卡"
                    }
                }
                else -> return@launch
            }
            app.refreshWidgets()
            Toast.makeText(this@ShortcutPunchActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
