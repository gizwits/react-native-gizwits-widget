package com.gizwitswidget.control

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.gizwitswidget.AppWidgetController
import com.gizwitswidget.R

class ControlWidgetProvider : AppWidgetProvider() {

    /**
     * 控制小组件事件回调接口
     * @param context 上下文对象
     * @param intent 事件意图
     */
    override fun onReceive(context: Context, intent: Intent) {
        // 处理小组件事件之前确保小组件视图已初始化
        AppWidgetController.activateAppWidget(context)
        // 分发小组件事件
        super.onReceive(context, intent)
        // 处理小组件点击事件
        when (intent.action) {
            ControlWidgetView.ACTION_EXECUTE_CONTROL -> {
                val deviceId: String = intent.getStringExtra(ControlWidgetView.EXTRA_CONTROL_DEVICE_ID) ?: return
                val controlId: Int = intent.getIntExtra(ControlWidgetView.EXTRA_CONTROL_ID, 0)
                val isSandBox: Boolean? = if (intent.hasExtra(ControlWidgetView.EXTRA_CONTROL_DEVICE_IS_SAND_BOX)) {
                    intent.getBooleanExtra(ControlWidgetView.EXTRA_CONTROL_DEVICE_IS_SAND_BOX, false)
                } else {
                    null
                }
                ControlWidgetView.onClickControlButton(deviceId, controlId, isSandBox)
            }
        }
    }

    /**
     * 当控制小组件被首次添加时，回调此方法，激活小组件相关服务
     * @param context 上下文对象
     */
    override fun onEnabled(context: Context) {
        ControlWidgetView.onEnabled()
    }

    /**
     * 当控制小组件被通知更新时，回调此方法，更新小组件的视图内容
     * @param context 上下文对象
     * @param appWidgetManager 小组件管理器
     * @param appWidgetIds 已被添加的小组件id集合
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 获取并更新小组件视图
        updateAppWidget(context)
    }

    /**
     * 当控制小组件被完全移除之后，回调此方法，释放小组件相关服务
     * @param context 上下文对象
     */
    override fun onDisabled(context: Context) {
        ControlWidgetView.onDisabled()
    }

    companion object {

        fun updateAppWidget(context: Context) {
            val component = ComponentName(context, ControlWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                val appWidgetIds: IntArray = getAppWidgetIds(component)
                if (appWidgetIds.isNotEmpty()) {
                    updateAppWidget(
                        appWidgetIds,
                        ControlWidgetView.onCreateView(context, appWidgetIds)
                    )
                }
            }
        }

        fun notifyAppWidgetViewDataChanged(context: Context) {
            val component = ComponentName(context, ControlWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                notifyAppWidgetViewDataChanged(getAppWidgetIds(component), R.id.control_list)
            }
        }

    }

}