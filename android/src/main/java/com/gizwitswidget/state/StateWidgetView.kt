package com.gizwitswidget.state

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.gizwitswidget.R
import com.gizwitswidget.WidgetRemoteViewsFactory
import com.gizwitswidget.model.StateContentFormatTitle
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.*

object StateWidgetView : WidgetRemoteViewsFactory() {

    /**
     * 持有应用实例
     */
    private lateinit var application: Application

    /**
     * 当前场景小组件的视图模型对象
     */
    private var viewModel: StateWidgetViewModel? = null

    /**
     * 状态小组件列表项状态列表
     */
    private var stateWidgetItemStateList: List<StateWidgetItemState> = listOf()

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
        viewModel = StateWidgetViewModel(application)
        isCreated = true
    }

    /**
     * 当状态小组件被首次添加时，回调此方法，激活小组件相关服务
     */
    fun onEnabled() = viewModel?.setEnabled(true)

    /**
     * 当前小组件的视图需要被创建、更新时回调此方法，此方法返回由最新小组件数据构建的视图
     * @param context 上下文对象
     * @param appWidgetIds 已被添加的小组件id集合
     * @return 新的小组件视图
     */
    fun onCreateView(context: Context, appWidgetIds: IntArray): RemoteViews {
        return RemoteViews(context.packageName, R.layout.state_widget_layout).apply {
            // 设备状态列表的挂起意图
            val adapterIntent: Intent = Intent(context, StateWidgetService::class.java)
            setRemoteAdapter(R.id.state_list, adapterIntent)
            // 设置空列表时的动作
            setEmptyView(R.id.state_list, R.id.empty_state_list_tip)
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
        }
    }

    override fun onDataSetChanged() {
        val stateWidgetUiState: StateWidgetUiState = viewModel?.stateWidgetUiState ?: StateWidgetUiState.Idle
        stateWidgetItemStateList = when (stateWidgetUiState) {
            is StateWidgetUiState.Idle -> listOf()
            is StateWidgetUiState.Success -> stateWidgetUiState.itemStateList
        }
    }

    override fun getCount(): Int = stateWidgetItemStateList.count()

    override fun getItemId(position: Int): Long {
        val stateWidgetItemState: StateWidgetItemState = stateWidgetItemStateList[position]
        return stateWidgetItemState.deviceId.hashCode() + stateWidgetItemState.stateId.toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val itemState: StateWidgetItemState = stateWidgetItemStateList[position]
        return RemoteViews(application.packageName, R.layout.state_widget_item).apply {
            // 设置状态组件名称
            setTextViewText(R.id.state_device_name, itemState.deviceName)
            // 设置状态组件名称的字体颜色
            setTextColor(
                R.id.state_device_name,
                if (itemState.isOnline) {
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
            if (itemState.isOnline) {
                run {
                    // 如果设备在线，则根据条件匹配决定显示的内容，如果当前状态值为空，则忽略（显示等待进度条）
                    val currentValue: JsonElement = itemState.attrsValue ?: return@run
                    // 根据条件匹配是否显示指定内容
                    if (itemState.contentList.isEmpty()) {
                        // 状态值比较匹配的数组为空，则直接显示状态字面量
                        isShowTextContent = true
                        textContentToShow = currentValue.toString()
                        return@run
                    }
                    // 迭代查询是否满足指定内容的条件
                    itemState.contentList.forEach contentLoop@ { stateContent ->
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
                            textContentToShow = itemState.language
                                .getAsJsonObject(itemState.languageKey)
                                ?.get(stateContent.text)
                                ?.asString ?: "null"
                            return@run
                        }
                    }
                    // 没有匹配的内容，则直接显示状态指定的图标或者字面量值
                    if (itemState.icon != null) {
                        isShowImageContent = true
                        imageContentToShow = itemState.icon
                    } else {
                        isShowTextContent = true
                        textContentToShow = currentValue.toString()
                    }
                }
            } else {
                // 如果设备离线，则直接显示离线字段
                isShowTextContent = true
                textContentToShow = application.getString(R.string.offline)
            }
            // 设置中心区域内容的可见性，决定显示的内容：文本、图片、进度条
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
            // 设置中心区域显示的内容颜色，决定中心区域显示内容的颜色
            setTextColor(
                R.id.state_text_content,
                if (itemState.isOnline) {
                    Color.WHITE
                } else {
                    Color.parseColor("#7FFFFFFF")
                }
            )
            // 设置中心区域显示的内容，显示图片或者文本
            if (isShowImageContent) {
                // 中心区域显示内容为图片
                try {
                    val stateImageBitmap: Bitmap = Glide.with(application)
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
                if (itemState.title != null) {
                    title = itemState.language
                        .getAsJsonObject(itemState.languageKey)
                        ?.get(itemState.title.textId)
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
                if (itemState.isOnline) {
                    Color.WHITE
                } else {
                    Color.parseColor("#7FFFFFFF")
                }
            )
        }
    }

    /**
     * 当小组件被完全移除之后，回调此方法，释放小组件相关服务
     */
    fun onDisabled() = viewModel?.setEnabled(false)

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
    val firstValue: Double = firstPrimitive.asDouble
    val secondValue: Double = secondPrimitive.asDouble
    return firstValue.compareTo(secondValue)
}









