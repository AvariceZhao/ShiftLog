package com.clockin.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockin.app.ClockInApplication

/** 开机后恢复小组件定时刷新与上班时间闹钟 */
class WidgetBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? ClockInApplication ?: return
        app.scheduleWidgetRefreshJobs()
    }
}
