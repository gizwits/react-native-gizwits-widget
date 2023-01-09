package com.gizwitswidget.state

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.ProgressBar
import android.widget.RemoteViews
import android.widget.TextView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bumptech.glide.Glide
import com.gizwitswidget.AppWidgetService
import com.gizwitswidget.R
import com.gizwitswidget.configurationStore
import com.gizwitswidget.model.StateConfiguration
import com.gizwitswidget.model.StateContentFormatTitle
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.gizwitswidget.model.AppWidgetConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.suspendCoroutine

object StateWidgetController {

    private const val TAG: String = "StateWidgetController"

    /**
     * 关联应用小组件控制器的协程作用域
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * 用于解析传入的小组件相关配置信息的解析器
     */
    private val configurationParser: Gson = GsonBuilder().create()

    /**
     * 本地键值对存储的Key，用于检索存储的状态小组件配置信息
     */
    private val STATE_CONFIGURATION_KEY = stringPreferencesKey("stateConfiguration")

    /**
     * 状态组件的列表项
     */
    private var stateWidgetItemList: List<StateWidgetItem> = emptyList()

    /**
     * 注册状态小组件的配置信息
     * @param context 应用上下文
     * @param widgetConfiguration 应用小组件配置信息
     * @param configuration 状态小组件的配置信息
     */
    fun registerStateConfiguration(
        context: Context,
        widgetConfiguration: AppWidgetConfiguration,
        configuration: String? = null
    ) {
        // 启动协程初始化解析状态小组件的配置信息
        scope.launch {
            val stateConfiguration: String = configuration ?: context
                .configurationStore.data.first()[STATE_CONFIGURATION_KEY] ?: return@launch
            try {
                // 解析配置信息
                val collectionType: TypeToken<List<StateConfiguration>> =
                    object : TypeToken<List<StateConfiguration>>() {}
                val stateConfigurationList: List<StateConfiguration> = configurationParser
                    .fromJson(stateConfiguration, collectionType.type)
                // 保存状态小组件配置信息
                context.configurationStore.edit {
                    it[STATE_CONFIGURATION_KEY] = stateConfiguration
                }
                // 初始化状态选项列表对象
                stateWidgetItemList = stateConfigurationList.map {
                    StateWidgetItem(
                        applicationContext = context,
                        appWidgetConfiguration = widgetConfiguration,
                        configuration = it
                    )
                }
                // 注册状态小组件服务，管理设备的在线、数据点状态
                AppWidgetService.registerStateService(
                    stateConfigurations = stateConfigurationList,
                    onDeviceConnectionChanged = { deviceId, isOnline ->
                        // 设备连接状态发生变更，通知小组件更新修改状态
                        onConnectionStateChanged(context, deviceId, isOnline)
                    },
                    onDeviceStateChanged = { deviceId, attributes ->
                        // 设备的数据点状态发生变更，通知小组件更新修改状态
                        onDeviceStateChanged(context, deviceId, attributes)
                    }
                )
                // 通知状态小组件列表已更新
                StateWidgetProvider.updateSceneWidgetListView(context)
            } catch (e: Exception) {
                // 发生未知错误
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取状态小组件的配置信息
     * @param context 应用上下文
     * @param callback 回调配置信息
     */
    fun getStateConfiguration(context: Context, callback: (String) -> Unit) {
        scope.launch {
            val stateConfiguration: String = context.configurationStore.data
                .first()[STATE_CONFIGURATION_KEY] ?: ""
            callback(stateConfiguration)
        }
    }

    /**
     * 获取状态小组件的根视图
     * @param context 应用上下文
     * @param appWidgetId 状态小组件的ID
     * @return 小组件的根视图
     */
    fun getStateWidgetView(context: Context, appWidgetId: Int): RemoteViews {
        return RemoteViews(
            context.packageName,
            R.layout.state_widget_layout
        ).apply {
            val remoteAdapterIntent: Intent =
                Intent(context, StateWidgetService::class.java)
            setRemoteAdapter(R.id.state_list, remoteAdapterIntent)
            setEmptyView(R.id.state_list, R.id.empty_state_list_tip)
        }
    }

    /**
     * 获取状态小组件的子列表视图
     * @return 状态小组件的子列表视图
     */
    fun getStateWidgetItemList(): List<StateWidgetItem> = stateWidgetItemList

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
        stateWidgetItemList.forEach {
            if (it.deviceId == deviceId && it.isOnline != isOnline) {
                it.isOnline = isOnline
                isViewChange = true
            }
        }
        if (isViewChange) {
            StateWidgetProvider.updateSceneWidgetListView(context)
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
        stateWidgetItemList.forEach {
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
            StateWidgetProvider.updateSceneWidgetListView(context)
        }
    }

}

class StateWidgetItem(
    private val applicationContext: Context,
    private val appWidgetConfiguration: AppWidgetConfiguration,
    private val configuration: StateConfiguration
) {

    val stateId: Int = configuration.id

    val deviceId: String = configuration.deviceId

    val attrsKey: String = configuration.attrsKey

    var attrsValue: JsonElement? = null

    var isOnline: Boolean = false

    fun getView(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.state_widget_item).apply {
            // 设置状态组件名称
            val deviceName: String = configuration.language
                .getAsJsonObject(appWidgetConfiguration.languageKey)
                ?.get(configuration.nameId)
                ?.asString ?: ""
            setTextViewText(R.id.state_device_name, deviceName)
            // 设置状态组件名称的字体颜色
            setTextColor(
                R.id.state_device_name,
                if (isOnline) {
                    Color.WHITE
                } else {
                    Color.parseColor("#7FFFFFFF")
                }
            )
            // 设置状态组件显示的内容
            var isShowTextContent: Boolean = false
            var isShowImageContent: Boolean = false
            var textContentToShow: String? = null
            var imageContentToShow: String? = null
            var formatTitleToShow: StateContentFormatTitle? = null
            if (isOnline) {
                run {
                    // 如果设备在线，则根据条件匹配决定显示的内容，如果当前状态值为空，则忽略
                    val currentValue: JsonElement = attrsValue ?: return@run
                    // 根据条件匹配是否显示指定内容
                    if (configuration.contentList.isEmpty()) {
                        // 状态值比较匹配的数组为空，则直接显示状态字面量
                        isShowTextContent = true
                        textContentToShow = currentValue.toString()
                        return@run
                    }
                    // 迭代查询是否满足指定内容的条件
                    configuration.contentList.forEach contentLoop@ { stateContent ->
                        // 检索是否满足显示当前内容的条件
                        stateContent.conditions.forEach { condition ->
                            val isMatched: Boolean = when(condition.operation) {
                                "==" -> currentValue == condition.value
                                "!=" -> currentValue != condition.value
                                ">" -> currentValue > condition.value
                                ">=" -> currentValue >= condition.value
                                "<" -> currentValue < condition.value
                                "<=" -> currentValue <= condition.value
                                else -> throw RuntimeException("Unsupported operator")
                            }
                            if (!isMatched) {
                                // 只要有一个条件不匹配，则不显示当前内容
                                return@contentLoop
                            }
                        }
                        // 所有的条件匹配成功，显示指定的内容
                        formatTitleToShow = stateContent.formatTitle
                        if (stateContent.image != null) {
                            isShowImageContent = true
                            imageContentToShow = stateContent.image
                            return@run
                        } else if (stateContent.text != null) {
                            isShowTextContent = true
                            textContentToShow = stateContent.text
                            return@run
                        }
                    }
                    // 没有匹配的内容，则直接显示状态指定的图标或者字面量值
                    if (configuration.icon != null) {
                        isShowImageContent = true
                        imageContentToShow = configuration.icon
                    } else {
                        isShowTextContent = true
                        textContentToShow = currentValue.toString()
                    }
                }
            } else {
                // 如果设备离线，则直接显示离线字段
                isShowTextContent = true
                textContentToShow = context.getString(R.string.offline)
            }
            // 设置中心区域内容的可见性
            setViewVisibility(
                R.id.state_text_content,
                if (isShowTextContent) View.VISIBLE else View.INVISIBLE
            )
            setViewVisibility(
                R.id.state_image_content,
                if (isShowImageContent) View.VISIBLE else View.INVISIBLE
            )
            setViewVisibility(
                R.id.state_empty_content,
                if (!isShowTextContent && !isShowImageContent) View.VISIBLE else View.INVISIBLE
            )
            // 设置中心区域显示的内容颜色
            setTextColor(
                R.id.state_text_content,
                if (isOnline) {
                    Color.WHITE
                } else {
                    Color.parseColor("#7FFFFFFF")
                }
            )
            // 设置中心区域显示的内容
            if (isShowImageContent) {
                // 中心区域显示内容为图片
                try {
                    val stateImageBitmap: Bitmap = Glide.with(context)
                        .asBitmap()
                        .load(imageContentToShow)
                        .submit()
                        .get()
                    setImageViewBitmap(R.id.state_image_content, stateImageBitmap)
                } catch (e: Exception) {
                    // TODO 请求图标失败，尝试重新获取
                }
            } else if (isShowTextContent) {
                // 中心区域显示内容为文本
                setTextViewText(R.id.state_text_content, textContentToShow)
            }
            // 设置状态组件标题，根据实际条件显示不同内容的标题
            var title: String? = null
            if (formatTitleToShow != null) {
                // formatTitle字段不为空，则显示时间
                title = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
            } else {
                // formatTitle字段为空，如果存在title则显示title
                if (configuration.title != null) {
                    title = configuration.language
                        .getAsJsonObject(appWidgetConfiguration.languageKey)
                        ?.get(configuration.title.textId)
                        ?.asString ?: "null"
                }
            }
            if (title != null) {
                setTextViewText(R.id.state_title, title)
            }
            // 设置状态组件标题的可见性
            setViewVisibility(
                R.id.state_title,
                if (title != null) View.VISIBLE else View.INVISIBLE
            )
            // 设置状态组件标题的字体颜色
            setTextColor(
                R.id.state_title,
                if (isOnline) {
                    Color.WHITE
                } else {
                    Color.parseColor("#7FFFFFFF")
                }
            )
        }
    }

    /**
     * 获取当前状态值，如果状态值未初始化，则根据类型获取默认值
     */
    private fun getAttrsValueOrDefault(): JsonElement {
        return when {
            attrsValue != null -> attrsValue!!
            configuration.type == "Boolean" -> JsonPrimitive(false)
            configuration.type == "Number" -> JsonPrimitive(0)
            configuration.type == "Enumeration" -> JsonPrimitive(0)
            else -> throw RuntimeException("Unsupported type")
        }
    }

}

private operator fun JsonElement.compareTo(another: JsonElement): Int {
    if (!this.isJsonPrimitive || !another.isJsonPrimitive) {
        // 不是主要类型，无法比较
        throw RuntimeException("Unsupported type")
    }
    val firstPrimitive: JsonPrimitive = this as JsonPrimitive
    val secondPrimitive: JsonPrimitive = another as JsonPrimitive
    if (!firstPrimitive.isNumber || !secondPrimitive.isNumber) {
        // 不是数值类型，无法比较
        throw RuntimeException("Unsupported type")
    }
    val firstValue: Number = firstPrimitive.asNumber
    val secondValue: Number = secondPrimitive.asNumber
    return when (firstValue) {
        is Float, is Double -> {
            when (secondValue) {
                is Float, is Double -> firstValue.toDouble().compareTo(secondValue.toDouble())
                is Int, is Long -> firstValue.toLong().compareTo(secondValue.toLong())
                else -> throw java.lang.Exception()
            }
        }
        is Int, is Long -> {
            when (secondValue) {
                is Float, is Double -> firstValue.toDouble().compareTo(secondValue.toDouble())
                is Int, is Long -> firstValue.toLong().compareTo(secondValue.toLong())
                else -> throw java.lang.Exception()
            }
        }
        else -> throw java.lang.Exception()
    }
}

















