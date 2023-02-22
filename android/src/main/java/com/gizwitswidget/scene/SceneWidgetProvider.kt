package com.gizwitswidget.scene

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gizwitswidget.AppWidgetController
import com.gizwitswidget.R

class SceneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 更新应用场景小组件
        updateSceneWidgetView(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 接收到小组件相关广播时，尝试注册小组件控制器，避免之前应用已被杀死
        AppWidgetController.tryRegisterController(context)

        super.onReceive(context, intent)
        // 检索并执行场景小组件的场景点击执行事件
        when (intent.action) {
            SceneWidgetController.ACTION_EXECUTE_SCENE -> {
                val sceneId: Int = intent.getIntExtra(SceneWidgetController.EXTRA_SCENE_ID, 0)
                SceneWidgetController.executeScene(sceneId)
            }
        }
    }

    companion object {

        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, SceneWidgetProvider::class.java)
            return AppWidgetManager.getInstance(context).getAppWidgetIds(component).isNotEmpty()
        }

        fun updateSceneWidgetView(context: Context) {
            val component = ComponentName(context, SceneWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                getAppWidgetIds(component).forEach { appWidgetId ->
                    updateAppWidget(
                        appWidgetId,
                        SceneWidgetController.getSceneWidgetView(context, appWidgetId)
                    )
                }
            }
        }

        fun updateSceneWidgetListView(context: Context) {
            val component = ComponentName(context, SceneWidgetProvider::class.java)
            AppWidgetManager.getInstance(context).apply {
                notifyAppWidgetViewDataChanged(getAppWidgetIds(component), R.id.scene_list)
            }
        }

    }

}











