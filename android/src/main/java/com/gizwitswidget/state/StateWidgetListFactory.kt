package com.gizwitswidget.state

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory

class StateWidgetListFactory(
    private val applicationContext: Context
) : RemoteViewsFactory {

    private var stateWidgetItemList: List<StateWidgetItem> = emptyList()

    override fun onCreate() { }

    override fun onDestroy() { }

    override fun onDataSetChanged() {
        // 更新设备子状态组件列表
        stateWidgetItemList = StateWidgetController.getStateWidgetItemList()
    }

    override fun getCount(): Int = stateWidgetItemList.size

    override fun getViewAt(position: Int): RemoteViews {
        return stateWidgetItemList[position].getView(applicationContext)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        val stateWidgetItem: StateWidgetItem = stateWidgetItemList[position]
        return stateWidgetItem.deviceId.hashCode() + stateWidgetItem.stateId.toLong()
    }

    override fun hasStableIds(): Boolean = true

}
