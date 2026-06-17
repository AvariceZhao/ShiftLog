package com.clockin.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.clockin.app.domain.PunchResult
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
                ConfirmPunchActivity.PUNCH_IN -> when (repository.performClockIn()) {
                    PunchResult.Success -> "已上班打卡"
                    PunchResult.AlreadyClockedIn -> "今日已上班，无需重复打卡"
                    PunchResult.NotClockedInYet -> "请先上班打卡"
                }
                ConfirmPunchActivity.PUNCH_OUT -> when (repository.performClockOut()) {
                    PunchResult.Success -> "已下班打卡"
                    PunchResult.NotClockedInYet -> "请先上班打卡"
                    PunchResult.AlreadyClockedIn -> "请先上班打卡"
                }
                else -> return@launch
            }
            app.refreshWidgets()
            Toast.makeText(this@ShortcutPunchActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
