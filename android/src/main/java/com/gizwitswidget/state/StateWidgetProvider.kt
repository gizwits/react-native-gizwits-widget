package com.gizwitswidget.state

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.gizwitswidget.AppWidgetController
import com.gizwitswidget.R

class StateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 更新应用状态小组件
        updateStateWidgetView(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 接收到小组件相关广播时，尝试注册小组件控制器，避免之前应用已被杀死
        AppWidgetController.tryRegisterController(context)

        super.onReceive(context, intent)
    }

    companion object {

        fun updateStateWidgetView(context: Context) {
            val component = ComponentName(context, StateWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                getAppWidgetIds(component).forEach { appWidgetId ->
                    updateAppWidget(
                        appWidgetId,
                        StateWidgetController.getStateWidgetView(context, appWidgetId)
                    )
                }
            }
        }

        fun updateSceneWidgetListView(context: Context) {
            val component = ComponentName(context, StateWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                notifyAppWidgetViewDataChanged(getAppWidgetIds(component), R.id.state_list)
            }
        }

    }

}









