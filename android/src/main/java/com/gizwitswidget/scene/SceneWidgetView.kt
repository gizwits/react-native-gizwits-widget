package com.gizwitswidget.scene

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.gizwitswidget.R
import com.gizwitswidget.WidgetRemoteViewsFactory
import com.gizwitswidget.control.ControlWidgetItemState
import com.gizwitswidget.control.ControlWidgetView
import com.gizwitswidget.control.ControlWidgetViewModel

object SceneWidgetView : WidgetRemoteViewsFactory() {

    /**
     * 用于标识场景ID的键
     */
    const val EXTRA_SCENE_ID: String = "com.gizwitswidget.scene.extra.SCENE_ID"

    /**
     * 用于标识场景对应的家ID的键
     */
    const val EXTRA_HOME_ID: String = "com.gizwitswidget.scene.extra.HOME_ID"

    /**
     * 应用小组件执行场景的广播动作，需要额外附带[EXTRA_SCENE_ID]数据
     */
    const val ACTION_EXECUTE_SCENE: String = "com.gizwitswidget.scene.action.EXECUTE_SCENE"

    /**
     * 持有应用实例
     */
    private lateinit var application: Application

    /**
     * 当前场景小组件的视图模型对象
     */
    private var viewModel: SceneWidgetViewModel? = null

    /**
     * 场景小组件列表项状态列表
     */
    private var sceneWidgetItemStateList: List<SceneWidgetItemState> = listOf()

    /**
     * 标志位，指示当前小组件视图是否已经被初始化
     */
    @Volatile
    private var isCreated: Boolean = false

    /**
     * 当小组件视图被创建时调用
     */
    @Synchronized
    fun onCreate(context: Context) {
        if (isCreated) {
            return
        }
        application = context.applicationContext as Application
        viewModel = SceneWidgetViewModel(application)
        isCreated = true
    }

    /**
     * 当场景小组件被首次添加时，回调此方法，激活小组件相关服务
     */
    fun onEnabled() = viewModel?.setEnabled(true)

    /**
     * 当前小组件的视图需要被创建、更新时回调此方法，此方法返回由最新小组件数据构建的视图
     * @param context 上下文对象
     * @param appWidgetIds 已被添加的小组件id集合
     * @return 新的小组件视图
     */
    fun onCreateView(context: Context, appWidgetIds: IntArray): RemoteViews {
        return RemoteViews(context.packageName, R.layout.scene_widget_layout).apply {
            // 设备场景列表的挂起意图
            val adapterIntent: Intent = Intent(context, SceneWidgetService::class.java)
            setRemoteAdapter(R.id.scene_list, adapterIntent)
            // 设置空列表时的动作
            setEmptyView(R.id.scene_list, R.id.empty_scene_list_tip)
            // 设置空列表时的点击添加按钮的事件，点击启动智家应用
            val launchAppIntent: Intent = context.packageManager
                .getLaunchIntentForPackage("com.gizwits.xb") ?: return@apply
            val launchAppPendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                appWidgetIds.first(),
                launchAppIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            setOnClickPendingIntent(R.id.bt_launch_app, launchAppPendingIntent)
            // 设置场景列表点击意图模版
            val clickTemplateIntent: Intent = Intent(ACTION_EXECUTE_SCENE).apply {
                component = ComponentName(context, SceneWidgetProvider::class.java)
            }
            val clickTemplatePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetIds.first(),
                clickTemplateIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            // 关联场景动作列表点击意图模版至列表
            setPendingIntentTemplate(R.id.scene_list, clickTemplatePendingIntent)
        }
    }

    /**
     * 用户点击触发场景动作
     * @param homeId 家id
     * @param sceneId 场景id
     */
    fun onClickSceneButton(homeId: Int, sceneId: Int) = viewModel?.executeScene(homeId, sceneId)

    override fun onDataSetChanged() {
        val sceneWidgetUiState: SceneWidgetUiState = viewModel?.sceneWidgetUiState ?: SceneWidgetUiState.Idle
        sceneWidgetItemStateList = when (sceneWidgetUiState) {
            is SceneWidgetUiState.Idle -> listOf()
            is SceneWidgetUiState.Success -> sceneWidgetUiState.itemStateList
        }
    }

    override fun getCount(): Int = sceneWidgetItemStateList.count()

    override fun getItemId(position: Int): Long {
        val sceneWidgetItemState: SceneWidgetItemState = sceneWidgetItemStateList[position]
        return sceneWidgetItemState.sceneId.toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val itemState: SceneWidgetItemState = sceneWidgetItemStateList[position]
        return RemoteViews(application.packageName, R.layout.scene_widget_item).apply {
            // 设置场景标题
            setTextViewText(R.id.scene_title, itemState.sceneTitle)
            // 设置场景背景
            setInt(R.id.scene_button, "setBackgroundResource", itemState.backgroundResId)
            // 设置场景图标
            val iconName: String = if (itemState.iconName.isNotEmpty()) {
                "ic_${itemState.iconName}"
            } else {
                "ic_huijia"
            }
            setInt(
                R.id.scene_button,
                "setImageResource",
                application.resources.getIdentifier(
                    iconName,
                    "drawable",
                    application.packageName
                )
            )
            // 设置场景图标的颜色
            setInt(
                R.id.scene_button,
                "setColorFilter",
                itemState.iconTint
            )
            // 设置场景被点击行为
            val fillInIntent: Intent = Intent().apply {
                // 设置家ID
                putExtra(EXTRA_HOME_ID, itemState.homeId)
                // 设置场景ID
                putExtra(EXTRA_SCENE_ID, itemState.sceneId)
            }
            setOnClickFillInIntent(R.id.scene_button, fillInIntent)
        }
    }

    /**
     * 当场景小组件被完全移除之后，回调此方法，释放小组件相关服务
     */
    fun onDisabled() = viewModel?.setEnabled(false)

}









