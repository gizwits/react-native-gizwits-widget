package com.gizwitswidget.scene

import android.content.Intent
import android.widget.RemoteViewsService

class SceneWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = SceneWidgetView

}
