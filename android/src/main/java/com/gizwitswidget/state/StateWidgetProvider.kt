package com.gizwitswidget.state

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.gizwitswidget.AppWidgetController
import com.gizwitswidget.R
import com.gizwitswidget.scene.SceneWidgetProvider
import com.gizwitswidget.scene.SceneWidgetView

class StateWidgetProvider : AppWidgetProvider() {

    /**
     * 状态小组件事件回调接口
     */
    override fun onReceive(context: Context, intent: Intent?) {
        // 处理小组件事件之前确保小组件视图已初始化
        AppWidgetController.activateAppWidget(context)
        // 分发小组件事件
        super.onReceive(context, intent)
    }

    /**
     * 当状态小组件被首次添加时，回调此方法，激活小组件相关服务
     * @param context 上下文对象
     */
    override fun onEnabled(context: Context?) = StateWidgetView.onEnabled()

    /**
     * 当状态小组件被通知更新时，回调此方法，更新小组件的视图内容
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
     * 当状态小组件被完全移除之后，回调此方法，释放小组件相关服务
     * @param context 上下文对象
     */
    override fun onDisabled(context: Context?) = StateWidgetView.onDisabled()

    companion object {

        fun updateAppWidget(context: Context) {
            val component = ComponentName(context, StateWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                val appWidgetIds: IntArray = getAppWidgetIds(component)
                if (appWidgetIds.isNotEmpty()) {
                    updateAppWidget(
                        appWidgetIds,
                        StateWidgetView.onCreateView(context, appWidgetIds)
                    )
                }
            }
        }

        fun notifyAppWidgetViewDataChanged(context: Context) {
            val component = ComponentName(context, StateWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                notifyAppWidgetViewDataChanged(getAppWidgetIds(component), R.id.state_list)
            }
        }

    }

}














