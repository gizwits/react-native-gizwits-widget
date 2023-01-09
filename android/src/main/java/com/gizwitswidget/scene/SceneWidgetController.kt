package com.gizwitswidget.scene

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gizwitswidget.R
import com.gizwitswidget.configurationStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.gizwits.xb.model.AppWidgetConfiguration
import com.gizwits.xb.model.SceneConfiguration
import com.gizwits.xb.network.AppWidgetApi
import com.gizwits.xb.network.HeaderManageInterceptor
import com.gizwits.xb.network.WidgetApiResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SceneWidgetController {

    /**
     * 用于标识场景ID的键
     */
    const val EXTRA_SCENE_ID: String = "com.gizwits.xb.extra.SCENE_ID"

    /**
     * 应用小组件执行场景的广播动作，需要额外附带[EXTRA_SCENE_ID]数据
     */
    const val ACTION_EXECUTE_SCENE: String = "com.gizwits.xb.action.EXECUTE_SCENE"

    /**
     * 关联应用小组件控制器的协程作用域
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * 用于解析传入的小组件相关配置信息的解析器
     */
    private val configurationParser: Gson = GsonBuilder().create()

    /**
     * 本地键值对存储的Key，用于检索存储的场景小组件配置信息
     */
    private val SCENE_CONFIGURATION_KEY = stringPreferencesKey("sceneConfiguration")

    /**
     * 标志位，表示当前场景组件是否已经注册
     */
    private val isRegisteredState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * 场景组件的列表项
     */
    private var sceneWidgetItemList: List<SceneWidgetItem> = emptyList()

    /**
     * 注册场景小组件的配置信息
     * @param context 应用上下文
     * @param widgetConfiguration 应用小组件配置信息
     * @param configuration 场景小组件的配置信息
     */
    fun registerSceneConfiguration(
        context: Context,
        widgetConfiguration: AppWidgetConfiguration,
        configuration: String? = null
    ) {
        // 启动协程初始化解析场景小组件的配置信息
        scope.launch {
            val sceneConfiguration: String = configuration ?: context
                .configurationStore.data.first()[SCENE_CONFIGURATION_KEY] ?: return@launch
            try {
                // 解析配置信息
                val collectionType: TypeToken<List<SceneConfiguration>> =
                    object : TypeToken<List<SceneConfiguration>>() {}
                val sceneConfigurationList: List<SceneConfiguration> = configurationParser
                    .fromJson(sceneConfiguration, collectionType.type)
                // 保存场景小组件配置信息
                context.configurationStore.edit {
                    it[SCENE_CONFIGURATION_KEY] = sceneConfiguration
                }
                // 初始化场景选项列表对象
                sceneWidgetItemList = sceneConfigurationList.map {
                    SceneWidgetItem(
                        applicationContext = context,
                        appWidgetConfiguration = widgetConfiguration,
                        configuration = it
                    )
                }
                // 通知场景小组件列表已经更新
                SceneWidgetProvider.updateSceneWidgetListView(context)
            } catch (e: Exception) {
                // 发生未知错误
                e.printStackTrace()
            } finally {
                isRegisteredState.value = true
            }
        }
    }

    /**
     * 获取场景小组件的配置信息
     * @param context 应用上下文
     * @param callback 回调配置信息
     */
    fun getSceneConfiguration(context: Context, callback: (String) -> Unit) {
        scope.launch {
            val sceneConfiguration: String = context.configurationStore.data
                .first()[SCENE_CONFIGURATION_KEY] ?: ""
            callback(sceneConfiguration)
        }
    }

    fun getSceneWidgetView(context: Context, appWidgetId: Int): RemoteViews {
        return RemoteViews(
            context.packageName,
            R.layout.scene_widget_layout
        ).apply {
            val remoteAdapterIntent: Intent = Intent(context, SceneWidgetService::class.java)
            setRemoteAdapter(R.id.scene_list, remoteAdapterIntent)
            setEmptyView(R.id.scene_list, R.id.empty_scene_list_tip)
            // 设置列表点击模板事件
            val templateIntent: Intent = Intent(ACTION_EXECUTE_SCENE).apply {
                component = ComponentName(context, SceneWidgetProvider::class.java)
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
            setPendingIntentTemplate(R.id.scene_list, templatePendingIntent)
        }
    }

    fun getSceneWidgetItemList(): List<SceneWidgetItem> = sceneWidgetItemList

    fun executeScene(sceneId: Int) {
        scope.launch {
            isRegisteredState.first { it }      // 等待场景组件注册完成

            val sceneWidgetItem: SceneWidgetItem = sceneWidgetItemList
                .firstOrNull { it.sceneId == sceneId } ?: return@launch
            sceneWidgetItem.executeScene()
        }
    }

}

class SceneWidgetItem(
    private val applicationContext: Context,
    private val appWidgetConfiguration: AppWidgetConfiguration,
    private val configuration: SceneConfiguration
) {

    val sceneId: Int = configuration.id

    private var title: String = configuration.name

    private var iconTint: Int = Color.WHITE

    private var backgroundResId: Int = R.drawable.scene_widget_item_background

    private var isActiveState: Boolean = false

    private val appWidgetApi: AppWidgetApi = Retrofit.Builder()
        .baseUrl(appWidgetConfiguration.aepUrl)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HeaderManageInterceptor(appWidgetConfiguration.appId, appWidgetConfiguration.userToken)
                ).build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AppWidgetApi::class.java)


    suspend fun executeScene() {
        if (isActiveState) {
            return
        }
        isActiveState = true
        try {
            // 显示场景执行中状态
            switchToExecutionState()
            // 请求执行场景
            val response: WidgetApiResponse = appWidgetApi.executeUserScene(configuration.homeId, sceneId)
            if (response.isSuccess()) {
                switchToSuccessState()
            } else {
                switchToFailureState()
            }
        } catch (e: Exception) {
            switchToFailureState()
        } finally {
            withContext(NonCancellable) {
                delay(2000)
                switchToCommonState()
            }
            isActiveState = false
        }
    }

    fun getView(context: Context): RemoteViews {
         return RemoteViews(
            context.packageName,
            R.layout.scene_widget_item
        ).apply {
            // 设置场景标题
             setTextViewText(R.id.scene_title, title)
             // 设置场景背景
             setInt(R.id.scene_button, "setBackgroundResource", backgroundResId)
             // 设置场景图标
             setInt(
                 R.id.scene_button,
                 "setImageResource",
                 context.resources.getIdentifier(
                     "ic_${configuration.iconName}",
                     "drawable",
                     context.packageName
                 )
             )
             // 设置场景图标的颜色
             setInt(
                 R.id.scene_button,
                 "setColorFilter",
                 iconTint
             )
             // 设置场景被点击行为
             val fillInIntent: Intent = Intent().apply {
                 // 设置场景ID
                 putExtra(SceneWidgetController.EXTRA_SCENE_ID, configuration.id)
             }
             setOnClickFillInIntent(R.id.scene_button, fillInIntent)
         }
    }

    /**
     * 切换至普通状态
     */
    private fun switchToCommonState() {
        title = configuration.name
        iconTint = Color.WHITE
        backgroundResId = R.drawable.scene_widget_item_background
        updateSceneWidgetListView()
    }

    /**
     * 切换至执行中状态
     */
    private fun switchToExecutionState() {
        title = applicationContext.resources.getString(R.string.hint_in_execution)
        iconTint = Color.parseColor(appWidgetConfiguration.tintColor)
        backgroundResId = R.drawable.scene_widget_item_active_background
        updateSceneWidgetListView()
    }

    /**
     * 切换至执行失败状态
     */
    private fun switchToFailureState() {
        title = applicationContext.resources.getString(R.string.hint_execution_failure)
        iconTint = Color.parseColor(appWidgetConfiguration.tintColor)
        backgroundResId = R.drawable.scene_widget_item_active_background
        updateSceneWidgetListView()
    }

    /**
     * 切换至执行失败状态
     */
    private fun switchToSuccessState() {
        title = applicationContext.resources.getString(R.string.hint_execution_success)
        iconTint = Color.parseColor(appWidgetConfiguration.tintColor)
        backgroundResId = R.drawable.scene_widget_item_active_background
        updateSceneWidgetListView()
    }

    private fun updateSceneWidgetListView() {
        SceneWidgetProvider.updateSceneWidgetListView(applicationContext)
    }

}
































