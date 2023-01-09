package com.gizwitswidget.control

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
import androidx.annotation.Px
import androidx.core.content.res.ResourcesCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bumptech.glide.Glide
import com.gizwitswidget.AppWidgetService
import com.gizwitswidget.R
import com.gizwitswidget.configurationStore
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.model.ControlConfig
import com.gizwitswidget.model.ControlConfiguration
import com.gizwitswidget.model.ControlOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ControlWidgetController {

    private const val TAG: String = "ControlWidgetController"

    /**
     * 用于表示被控制设备的ID的键
     */
    const val EXTRA_CONTROL_DEVICE_ID: String = "com.gizwitswidget.extra.CONTROL_DEVICE_ID"

    /**
     * 用于表示控制ID的键
     */
    const val EXTRA_CONTROL_ID: String = "com.gizwitswidget.extra.CONTROL_ID"

    /**
     * 应用小组件执行控制的广播动作，需要额外附带[EXTRA_CONTROL_ID]数据
     */
    const val ACTION_EXECUTE_CONTROL: String = "com.gizwitswidget.action.EXECUTE_CONTROL"

    /**
     * 关联应用小组件控制器的协程作用域
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * 用于解析传入的小组件相关配置信息的解析器
     */
    private val configurationParser: Gson = GsonBuilder().create()

    /**
     * 本地键值对存储的Key，用于检索存储的控制小组件配置信息
     */
    private val CONTROL_CONFIGURATION_KEY = stringPreferencesKey("controlConfiguration")

    /**
     * 标志位，表示当前控制组件是否已经注册
     */
    private val isRegisteredState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * 控制组件的列表项
     */
    private var controlWidgetItemList: List<ControlWidgetItem> = emptyList()

    /**
     * 注册控制小组件的配置信息
     * @param context 应用上下文
     * @param widgetConfiguration 应用小组件配置信息
     * @param configuration 控制小组件的配置信息
     */
    fun registerControlConfiguration(
        context: Context,
        widgetConfiguration: AppWidgetConfiguration,
        configuration: String? = null
    ) {
        // 启动协程初始化解析控制小组件的配置信息
        scope.launch {
            val controlConfiguration: String = configuration ?: context
                .configurationStore.data.first()[CONTROL_CONFIGURATION_KEY] ?: return@launch
            try {
                // 解析配置信息
                val collectionType: TypeToken<List<ControlConfiguration>> =
                    object : TypeToken<List<ControlConfiguration>>() {}
                val controlConfigurationList: List<ControlConfiguration> = configurationParser
                    .fromJson(controlConfiguration, collectionType.type)
                // 保存控制小组件配置信息
                context.configurationStore.edit {
                    it[CONTROL_CONFIGURATION_KEY] = controlConfiguration
                }
                // 初始化控制选项列表对象
                controlWidgetItemList = controlConfigurationList.flatMap {
                    it.configs.map { controlConfig ->
                        ControlWidgetItem(
                            applicationContext = context,
                            appWidgetConfiguration = widgetConfiguration,
                            controlConfiguration = it,
                            controlConfig = controlConfig
                        )
                    }
                }
                // 注册控制小组件服务，管理设备的在线、数据点状态
                AppWidgetService.registerControlService(
                    controlConfigurations = controlConfigurationList,
                    onDeviceConnectionChanged = { deviceId, isOnline ->
                        // 设备连接状态发生变更，通知小组件更新修改状态
                        onConnectionStateChanged(context, deviceId, isOnline)
                    },
                    onDeviceInformationChanged = { deviceId, deviceName ->
                        // 设备信息发送变更，通知小组件更新修改信息
                        onDeviceInformationChanged(context, deviceId, deviceName)
                    },
                    onDeviceStateChanged = { deviceId, attributes ->
                        // 设备的数据点状态发生变更，通知小组件更新修改状态
                        onDeviceStateChanged(context, deviceId, attributes)
                    }
                )
                // 通知控制小组件列表已经更新
                ControlWidgetProvider.updateControlWidgetListView(context)
            } catch (e: Exception) {
                // 发生未知错误
                e.printStackTrace()
            } finally {
                isRegisteredState.value = true
            }
        }
    }

    /**
     * 获取控制小组件的配置信息
     * @param context 应用上下文
     * @param callback 回调配置信息
     */
    fun getControlConfiguration(context: Context, callback: (String) -> Unit) {
        scope.launch {
            val controlConfiguration: String = context.configurationStore.data
                .first()[CONTROL_CONFIGURATION_KEY] ?: ""

            val collectionType: TypeToken<List<ControlConfiguration>> =
                object : TypeToken<List<ControlConfiguration>>() {}
            val controlConfigurationList: List<ControlConfiguration> = configurationParser
                .fromJson(controlConfiguration, collectionType.type)
            callback(controlConfiguration)
        }
    }

    /**
     * 获取控制小组件的根视图
     * @param context 应用上下文
     * @param appWidgetId 控制小组件的ID
     * @return 小组件的根视图
     */
    fun getControlWidgetView(context: Context, appWidgetId: Int): RemoteViews {
        return RemoteViews(
            context.packageName,
            R.layout.control_widget_layout
        ).apply {
            val remoteAdapterIntent: Intent = Intent(context, ControlWidgetService::class.java)
            setRemoteAdapter(R.id.control_list, remoteAdapterIntent)
            setEmptyView(R.id.control_list, R.id.empty_control_list_tip)
            // 设置列表点击模板事件
            val templateIntent: Intent = Intent(ACTION_EXECUTE_CONTROL).apply {
                component = ComponentName(context, ControlWidgetProvider::class.java)
            }
            val templatePendingIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                templateIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            setPendingIntentTemplate(R.id.control_list, templatePendingIntent)
        }
    }

    /**
     * 获取控制小组件的控制动作列表视图
     * @return 小组件的控制动作列表视图
     */
    fun getControlWidgetItemList(): List<ControlWidgetItem> = controlWidgetItemList

    fun executeControl(deviceId: String, controlId: Int) {
        scope.launch {
            isRegisteredState.first { it }      // 等待控制组件注册完成

            val controlWidgetItem: ControlWidgetItem = controlWidgetItemList
                .firstOrNull {
                    it.deviceId == deviceId && it.configId == controlId
                } ?: return@launch
            controlWidgetItem.executeControl()
        }
    }

    /**
     * 处理用户绑定设备的上下线消息
     * @param context 应用上下文对象
     * @param deviceId 设备ID
     * @param isOnline 标识符，标识当前设备是否在线
     */
    @Synchronized
    private fun onConnectionStateChanged(
        context: Context,
        deviceId: String,
        isOnline: Boolean
    ) {
        var isViewChange: Boolean = false
        controlWidgetItemList.forEach {
            if (it.deviceId == deviceId && it.isOnline != isOnline) {
                it.isOnline = isOnline
                isViewChange = true
            }
        }
        if (isViewChange) {
            ControlWidgetProvider.updateControlWidgetListView(context)
        }
    }

    /**
     * 处理用户绑定设备的信息变更消息
     * @param context 应用上下文对象
     * @param deviceId 设备ID
     * @param deviceName 设备名称
     */
    @Synchronized
    private fun onDeviceInformationChanged(
        context: Context,
        deviceId: String,
        deviceName: String
    ) {
        var isViewChange: Boolean = false
        controlWidgetItemList.forEach {
            if (it.deviceId != deviceId) {
                return@forEach
            }
            if (it.deviceName == null) {
                it.deviceName = deviceName
                isViewChange = true
            }
        }
        if (isViewChange) {
            ControlWidgetProvider.updateControlWidgetListView(context)
        }
    }

    /**
     * 处理用户绑定设备的状态数据变更消息
     * @param context 应用上下文对象
     * @param deviceId 设备ID
     * @param attributes 设备状态数据
     */
    @Synchronized
    private fun onDeviceStateChanged(
        context: Context,
        deviceId: String,
        attributes: JsonObject
    ) {
        var isViewChange: Boolean = false
        controlWidgetItemList.forEach {
            if (it.deviceId != deviceId) {
                return@forEach
            }
            val value: JsonPrimitive = attributes
                .getAsJsonPrimitive(it.attrsKey) ?: return@forEach
            if (it.attrsValue != value) {
                it.attrsValue = value
                isViewChange = true
            }
        }
        if (isViewChange) {
            ControlWidgetProvider.updateControlWidgetListView(context)
        }
    }

}

class ControlWidgetItem(
    private val applicationContext: Context,
    private val appWidgetConfiguration: AppWidgetConfiguration,
    private val controlConfiguration: ControlConfiguration,
    private val controlConfig: ControlConfig
) {

    val configId: Int = controlConfig.id.toInt()

    val deviceId: String = controlConfiguration.deviceId

    var deviceName: String? = controlConfiguration.name

    val attrsKey: String = controlConfig.attrsKey

    var attrsValue: JsonElement? = null

    var isOnline: Boolean = false

    fun executeControl() {
        // 根据当前状态获取下一项控制的状态
        val newAttrsValue: JsonElement = when (controlConfig.type) {
            "Boolean" -> {
                // 如果是布尔值，则在false和true之间切换，初始状态为true
                val oldValue: Boolean? = attrsValue?.asBoolean
                when (oldValue) {
                    null -> JsonPrimitive(true)
                    else -> JsonPrimitive(!oldValue)
                }
            }
            else -> {
                // 如果不是布尔值，则在选项数组之间循环切换，初始状态为索引为0的选项
                val newIndex: Int = when {
                    attrsValue == null -> 0     // 当前状态值为null，则索引为0
                    else -> {
                        controlConfig.options.indexOfFirst {
                            it.value == attrsValue
                        }.let {
                            // 获取旧值的索引并加1，如果新索引在选项数组范围内则应用，否则重置为0
                            val index: Int = it + 1
                            if (index in 0 until controlConfig.options.size) {
                                index
                            } else {
                                0
                            }
                        }
                    }
                }
                controlConfig.options[newIndex].value
            }
        }
        // 执行并更新设备状态
        attrsValue = newAttrsValue
        val controlAttributesBody: JsonObject = JsonObject().apply {
            add(attrsKey, attrsValue)
        }
        AppWidgetService.executeControl(deviceId, controlAttributesBody)
        ControlWidgetProvider.updateControlWidgetListView(applicationContext)
    }

    fun getView(context: Context): RemoteViews {
        return RemoteViews(
            applicationContext.packageName,
            R.layout.control_widget_item
        ).apply {
            // 设置控制设备的名称：在线则显示设备名称，否则显示离线字样
            if (isOnline) {
                if (deviceName != null) {
                    // 配置信息的名称不为空，则优先使用
                    setTextViewText(R.id.control_device_name, deviceName)
                }
            } else {
                setTextViewText(
                    R.id.control_device_name,
                    applicationContext.getString(R.string.offline)
                )
            }
            // 设置控制设备的动作名称
            val actionNameId: String = controlConfig.nameId
            val actionName: String = controlConfiguration.language
                .getAsJsonObject(appWidgetConfiguration.languageKey)
                ?.get(actionNameId)
                ?.asString ?: "null"
            setTextViewText(R.id.control_action_name, actionName)
            // 设置控制设备右上角在线状态图标显示：在线则隐藏、离线则显示
            setViewVisibility(
                R.id.control_device_offline_icon,
                if (!isOnline) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
            )
            // 计算当前组件的状态
            var actionIconUrl: String = controlConfig.attrsIcon     // 设备控制动作的显示图标
            var isActionActivated: Boolean = false                  // 是否匹配设备控制动作的选项（决定是否高亮）
            val value: JsonElement? = attrsValue
            if (value != null && isOnline) {
                when {
                    controlConfig.type == "Boolean" -> {
                        // 类型为布尔值，进行特殊处理
                        val options: List<ControlOption> = controlConfig.options.filterNot {
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
                        val options: List<ControlOption> = controlConfig.options.filterNot {
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
                val deviceIcon: Bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(controlConfiguration.icon)
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
                    Color.parseColor(appWidgetConfiguration.tintColor)
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
                val actionIcon: Bitmap = Glide.with(applicationContext)
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
                context.resources,
                R.drawable.control_button_background,
                null
            )?.apply {
                if (isActionActivated) {
                    setTint(Color.parseColor(appWidgetConfiguration.tintColor))
                } else {
                    setTint(Color.WHITE)
                }
                setImageViewBitmap(R.id.control_button_background, toBitmap(200, 200))
            }
            // 设置控制选项被点击行为
            val fillInIntent: Intent = Intent().apply {
                // 设置控制设备的ID
                putExtra(ControlWidgetController.EXTRA_CONTROL_DEVICE_ID, controlConfiguration.deviceId)
                // 设置控制ID
                putExtra(ControlWidgetController.EXTRA_CONTROL_ID, configId)
            }
            setOnClickFillInIntent(R.id.control_button, fillInIntent)
        }
    }

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
















