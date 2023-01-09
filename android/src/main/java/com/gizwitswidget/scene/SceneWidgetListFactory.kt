package com.gizwitswidget.scene


import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory


class SceneWidgetListFactory(
    private val applicationContext: Context
) : RemoteViewsFactory {

    private var sceneWidgetItemList: List<SceneWidgetItem> = emptyList()

    override fun onCreate() { }

    override fun onDestroy() { }

    override fun onDataSetChanged() {
        // 更新场景列表
        sceneWidgetItemList = SceneWidgetController.getSceneWidgetItemList()
    }

    override fun getCount(): Int = sceneWidgetItemList.size

    override fun getViewAt(position: Int): RemoteViews {
        return sceneWidgetItemList[position].getView(applicationContext)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = sceneWidgetItemList[position].sceneId.toLong()

    override fun hasStableIds(): Boolean = true

}
