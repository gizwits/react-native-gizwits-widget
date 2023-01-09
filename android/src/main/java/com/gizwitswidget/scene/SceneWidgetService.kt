package com.gizwitswidget.scene

import android.content.Intent
import android.widget.RemoteViewsService

internal class SceneWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return SceneWidgetListFactory(applicationContext)
    }

}
