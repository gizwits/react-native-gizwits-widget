package com.gizwitswidget.control

import android.content.Intent
import android.widget.RemoteViewsService

internal class ControlWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ControlWidgetListFactory(applicationContext)
    }

}
