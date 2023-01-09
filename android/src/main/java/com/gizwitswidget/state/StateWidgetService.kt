package com.gizwitswidget.state

import android.content.Intent
import android.widget.RemoteViewsService

internal class StateWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StateWidgetListFactory(applicationContext)
    }

}
