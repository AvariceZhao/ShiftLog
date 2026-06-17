package com.clockin.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockin.app.ClockInApplication

private const val ACTION_TIME_SET = "android.intent.action.TIME_SET"

/** 日期 / 时区变化时刷新小组件，解决跨天后仍显示昨日状态的问题 */
class WidgetDayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_DATE_CHANGED,
            ACTION_TIME_SET,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                if (!WidgetRefreshScheduler.hasActiveWidgets(context)) return
                val app = context.applicationContext as? ClockInApplication ?: return
                app.scheduleWidgetRefreshJobs()
                app.refreshWidgets()
            }
        }
    }
}
