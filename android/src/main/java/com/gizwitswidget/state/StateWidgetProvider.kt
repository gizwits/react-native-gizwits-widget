package com.gizwitswidget.state

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import com.gizwitswidget.R

class StateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateStateWidgetView(context)
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









