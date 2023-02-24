package com.gizwitswidget

import android.widget.RemoteViews
import android.widget.RemoteViewsService

abstract class WidgetRemoteViewsFactory : RemoteViewsService.RemoteViewsFactory {

    override fun onCreate() {}

    override fun onDestroy() {}

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun hasStableIds(): Boolean = true

}