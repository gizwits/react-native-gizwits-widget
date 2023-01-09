package com.gizwitswidget.control

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.gizwitswidget.R

class ControlWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateControlWidgetView(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
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
