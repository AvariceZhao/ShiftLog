package com.clockin.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockin.app.ClockInApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 到达标准上班时间时刷新小组件，并预约下一次 */
class WidgetClockInAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != WidgetClockInAlarmScheduler.ACTION_CLOCK_IN_REFRESH) return
        if (!WidgetRefreshScheduler.hasActiveWidgets(context)) return
        val app = context.applicationContext as? ClockInApplication ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            try {
                app.refreshWidgetsNow()
                WidgetClockInAlarmScheduler.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
