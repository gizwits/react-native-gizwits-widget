package com.gizwitswidget.control

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.gizwitswidget.AppWidgetController
import com.gizwitswidget.R

class ControlWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 更新应用控制小组件
        updateControlWidgetView(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 接收到小组件相关广播时，尝试注册小组件控制器，避免之前应用已被杀死
        AppWidgetController.tryRegisterController(context)

        super.onReceive(context, intent)
        // 检索并执行控制小组件的动作点击执行事件
        when (intent.action) {
            ControlWidgetController.ACTION_EXECUTE_CONTROL -> {
                val deviceId: String = intent.getStringExtra(ControlWidgetController.EXTRA_CONTROL_DEVICE_ID) ?: return
                val controlId: Int = intent.getIntExtra(ControlWidgetController.EXTRA_CONTROL_ID, 0)
                ControlWidgetController.executeControl(deviceId, controlId)
            }
        }
    }

    companion object {

        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, ControlWidgetProvider::class.java)
            return AppWidgetManager.getInstance(context).getAppWidgetIds(component).isNotEmpty()
        }

        fun updateControlWidgetView(context: Context) {
            val component = ComponentName(context, ControlWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                getAppWidgetIds(component).forEach { appWidgetId ->
                    updateAppWidget(appWidgetId,
                        ControlWidgetController.getControlWidgetView(context, appWidgetId)
                    )
                }
            }
        }

        fun updateControlWidgetListView(context: Context) {
            val component = ComponentName(context, ControlWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                notifyAppWidgetViewDataChanged(getAppWidgetIds(component), R.id.control_list)
            }
        }

    }

}
