package com.gizwitswidget.control

import android.content.Intent
import android.widget.RemoteViewsService

class ControlWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = ControlWidgetView

}