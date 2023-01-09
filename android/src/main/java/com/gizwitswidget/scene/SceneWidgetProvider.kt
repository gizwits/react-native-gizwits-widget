package com.gizwitswidget.scene

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gizwitswidget.R
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.gizwits.xb.model.AppWidgetConfiguration
import com.gizwits.xb.model.SceneConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SceneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateSceneWidgetView(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
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











