package com.gizwitswidget.control

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService

internal class ControlWidgetListFactory(
    private val applicationContext: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var controlWidgetItemList: List<ControlWidgetItem> = emptyList()

    override fun onCreate() { }

    override fun onDestroy() { }

    override fun onDataSetChanged() {
        // 更新设备子控制组件列表
        controlWidgetItemList = ControlWidgetController.getControlWidgetItemList()
    }

    override fun getCount(): Int = controlWidgetItemList.size

    override fun getViewAt(position: Int): RemoteViews {
        return controlWidgetItemList[position].getView(applicationContext)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        val controlWidgetItem: ControlWidgetItem = controlWidgetItemList[position]
        return controlWidgetItem.deviceId.hashCode() + controlWidgetItem.configId.toLong()
    }

    override fun hasStableIds(): Boolean = true

}
