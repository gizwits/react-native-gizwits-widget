package com.gizwitswidget.control

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.Px
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.gizwitswidget.R
import com.gizwitswidget.WidgetRemoteViewsFactory
import com.gizwitswidget.WidgetViewModel
import com.gizwitswidget.model.ControlOption
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

object ControlWidgetView : WidgetRemoteViewsFactory() {

    /**
     * 用于表示被控制设备的ID的键
     */
    const val EXTRA_CONTROL_DEVICE_ID: String = "com.gizwitswidget.control.extra.CONTROL_DEVICE_ID"

    /**
     * 用于表示控制ID的键
     */
    const val EXTRA_CONTROL_ID: String = "com.gizwitswidget.control.extra.CONTROL_ID"

    /**
     * 用于表示被控设备的发布状态的键
     */
    const val EXTRA_CONTROL_DEVICE_IS_SAND_BOX: String = "com.gizwitswidget.control.extra.CONTROL_DEVICE_IS_SAND_BOX"

    /**
     * 应用小组件执行控制的广播动作，需要额外附带[EXTRA_CONTROL_ID]数据
     */
    const val ACTION_EXECUTE_CONTROL: String = "com.gizwitswidget.control.action.EXECUTE_CONTROL"

    /**
     * 持有应用实例
     */
    private lateinit var application: Application

    /**
     * 当前控制小组件的视图模型对象
     */
    private lateinit var viewModel: ControlWidgetViewModel

    /**
     * 控制小组件列表项状态列表
     */
    private var controlWidgetItemStateList: List<ControlWidgetItemState> = listOf()

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
        viewModel = ControlWidgetViewModel(application)
        isCreated = true
    }

    /**
     * 当控制小组件被首次添加时，回调此方法，激活小组件相关服务
     */
    fun onEnabled() = viewModel.setEnabled(true)

    /**
     * 当前小组件的视图需要被创建、更新时回调此方法，此方法返回由最新小组件数据构建的视图
     * @param context 上下文对象
     * @param appWidgetIds 已被添加的小组件id集合
     * @return 新的小组件视图
     */
    fun onCreateView(context: Context, appWidgetIds: IntArray): RemoteViews {
        return RemoteViews(context.packageName, R.layout.control_widget_layout).apply {
            // 设备控制动作列表的挂起意图
            val adapterIntent: Intent = Intent(context, ControlWidgetService::class.java)
            setRemoteAdapter(R.id.control_list, adapterIntent)
            // 设置空列表时的动作
            setEmptyView(R.id.control_list, R.id.empty_control_list_tip)
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
            // 设置控制动作列表点击意图模版
            val clickTemplateIntent: Intent = Intent(ACTION_EXECUTE_CONTROL).apply {
                component = ComponentName(context, ControlWidgetProvider::class.java)
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
            // 关联控制动作列表点击意图模版至列表
            setPendingIntentTemplate(R.id.control_list, clickTemplatePendingIntent)
        }
    }

    /**
     * 用户点击触发设备控制动作
     * @param deviceId 设备id
     * @param controlId 控制动作的id
     * @param isSandBox 被控设备的发布状态
     */
    fun onClickControlButton(deviceId: String, controlId: Int, isSandBox: Boolean?) =
        viewModel.executeControl(deviceId, controlId, isSandBox)

    override fun onDataSetChanged() {
        val controlWidgetUiState: ControlWidgetUiState = viewModel.controlWidgetUiState
        controlWidgetItemStateList = when (controlWidgetUiState) {
            is ControlWidgetUiState.Idle -> listOf()
            is ControlWidgetUiState.Success -> controlWidgetUiState.itemStateList
        }
    }

    override fun getCount(): Int = controlWidgetItemStateList.count()

    override fun getItemId(position: Int): Long {
        val controlWidgetItemState: ControlWidgetItemState = controlWidgetItemStateList[position]
        return controlWidgetItemState.deviceId.hashCode() + controlWidgetItemState.configId.toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val itemState: ControlWidgetItemState = controlWidgetItemStateList[position]
        return RemoteViews(
            application.packageName,
            R.layout.control_widget_item
        ).apply {
            // 显示设备名称
            setTextViewText(R.id.control_device_name, itemState.deviceName)
            // 设置控制设备的动作名称
            val actionNameId: String = itemState.nameId
            val actionName: String = itemState.language
                .getAsJsonObject(itemState.languageKey)
                ?.get(actionNameId)
                ?.asString ?: "null"
            setTextViewText(R.id.control_action_name, actionName)
            // 设置控制设备右上角在线状态图标显示：在线则隐藏、离线则显示
            setViewVisibility(
                R.id.control_device_offline_icon,
                if (!itemState.isOnline) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            )
            // 计算当前组件的状态
            var actionIconUrl: String = itemState.attrsIcon     // 设备控制动作的显示图标
            var isActionActivated: Boolean = false              // 是否匹配设备控制动作的选项（决定是否高亮）
            val value: JsonElement? = itemState.attrsValue
            if (value != null && itemState.isOnline) {
                when {
                    itemState.attrsType == "Boolean" -> {
                        // 类型为布尔值，进行特殊处理
                        val options: List<ControlOption> = itemState.options.filterNot {
                            it.notInOption
                        }
                        if (value is JsonPrimitive) {
                            when (options.size) {
                                // 匹配条件为空，则按照状态值处理
                                0 -> isActionActivated = if (value.isBoolean) value.asBoolean else false
                                // 匹配条件只有一个，则按照匹配处理
                                1 -> {
                                    val controlOption: ControlOption = options.first()
                                    isActionActivated = controlOption.value == value
                                    if (isActionActivated) {
                                        actionIconUrl = controlOption.imageUrl
                                    }
                                }
                                // 匹配条件有两个以上，则按照状态值处理
                                else -> {
                                    isActionActivated = if (value.isBoolean) value.asBoolean else false
                                    options.forEach {
                                        if (it.value == value) {
                                            actionIconUrl = it.imageUrl
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 其余值按照匹配条件处理
                    else -> {
                        val options: List<ControlOption> = itemState.options.filterNot {
                            it.notInOption
                        }
                        options.forEach {
                            if (it.value == value) {
                                isActionActivated = true
                                actionIconUrl = it.imageUrl
                            }
                        }
                    }
                }
            }
            // 设置控制选项的设备图标
            try {
                val deviceIcon: Bitmap = Glide.with(application)
                    .asBitmap()
                    .load(itemState.deviceIcon)
                    .submit()
                    .get()
                setImageViewBitmap(R.id.control_device_icon, deviceIcon)
            } catch (e: Exception) {
                // TODO 请求图标失败，尝试重新获取
            }
            // 设置控制选项的设备图标颜色
            setInt(
                R.id.control_device_icon,
                "setColorFilter",
                if (isActionActivated) {
                    Color.parseColor(itemState.tintColor)
                } else {
                    Color.WHITE
                }
            )
            // 设置控制选项的背景图片：如果处于激活状态（条件匹配），则背景显示白色，否则为透明颜色
            setInt(
                R.id.control_container,
                "setBackgroundResource",
                if (isActionActivated) {
                    R.drawable.control_widget_item_active_background
                } else {
                    R.drawable.control_widget_item_background
                }
            )
            // 设置控制选项的动作图标
            try {
                val actionIcon: Bitmap = Glide.with(application)
                    .asBitmap()
                    .load(actionIconUrl)
                    .submit()
                    .get()
                setImageViewBitmap(R.id.control_button, actionIcon)
            } catch (e: Exception) {
                // TODO 请求图标失败，尝试重新获取
            }
            // 设置控制选项的动作图标的背景颜色
            ResourcesCompat.getDrawable(
                application.resources,
                R.drawable.control_button_background,
                null
            )?.apply {
                if (isActionActivated) {
                    setTint(Color.parseColor(itemState.tintColor))
                } else {
                    setTint(Color.WHITE)
                }
                setImageViewBitmap(R.id.control_button_background, toBitmap(200, 200))
            }
            // 设置控制选项被点击行为
            val fillInIntent: Intent = Intent().apply {
                // 设置控制设备的ID
                putExtra(EXTRA_CONTROL_DEVICE_ID, itemState.deviceId)
                // 设置控制ID
                putExtra(EXTRA_CONTROL_ID, itemState.configId.toInt())
                // 设置被控设备的发布状态
                if (itemState.isSandbox != null) {
                    putExtra(EXTRA_CONTROL_DEVICE_IS_SAND_BOX, itemState.isSandbox)
                }
            }
            setOnClickFillInIntent(R.id.control_button, fillInIntent)
        }
    }

    /**
     * 当控制小组件被完全移除之后，回调此方法，释放小组件相关服务
     */
    fun onDisabled() = viewModel.setEnabled(false)

    private fun Drawable.toBitmap(
        @Px width: Int = intrinsicWidth,
        @Px height: Int = intrinsicHeight,
        config: Bitmap.Config? = null
    ): Bitmap {
        if (this is BitmapDrawable) {
            if (config == null || bitmap.config == config) {
                // Fast-path to return original. Bitmap.createScaledBitmap will do this check, but it
                // involves allocation and two jumps into native code so we perform the check ourselves.
                if (width == bitmap.width && height == bitmap.height) {
                    return bitmap
                }
                return Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
        }
        val oldLeft: Int = bounds.left
        val oldTop: Int = bounds.top
        val oldRight: Int = bounds.right
        val oldBottom: Int = bounds.bottom

        val bitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
        setBounds(0, 0, width, height)
        draw(Canvas(bitmap))

        setBounds(oldLeft, oldTop, oldRight, oldBottom)
        return bitmap
    }

}