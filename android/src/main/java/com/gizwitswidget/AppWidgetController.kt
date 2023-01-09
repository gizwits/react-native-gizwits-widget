package com.gizwitswidget

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.control.ControlWidgetController
import com.gizwitswidget.scene.SceneWidgetController
import com.gizwitswidget.state.StateWidgetController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal val Context.configurationStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_widget_configurations")

object AppWidgetController {

    private const val TAG: String = "AppWidgetController"

    /**
     * 应用上下文对象
     */
    private lateinit var applicationContext: Context

    /**
     * 应用小组件配置信息
     */
    private var appWidgetConfiguration: AppWidgetConfiguration? = null

    /**
     * 关联应用小组件控制器的协程作用域
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * 用于解析传入的小组件相关配置信息的解析器
     */
    private val configurationParser: Gson = GsonBuilder().create()

    /**
     * 本地键值对存储的Key，用于检索存储的小组件配置信息
     */
    private val WIDGET_CONFIGURATION_KEY = stringPreferencesKey("widgetConfiguration")

    /**
     * （外部接口）注册初始化应用小组件控制器
     * @param context 应用上下文对象
     */
    fun registerController(context: Context) {
        applicationContext = context.applicationContext
        registerWidgetConfiguration()
    }

    /**
     * 注册应用小组件的配置信息
     * @param configuration 应用小组件的配置信息
     */
    fun registerWidgetConfiguration(configuration: String? = null) {
        // 启动协程初始化解析应用小组件的配置信息
        scope.launch {
            val widgetConfiguration: String = configuration ?: applicationContext
                .configurationStore.data.first()[WIDGET_CONFIGURATION_KEY] ?: return@launch
            try {
                // 解析配置信息
                appWidgetConfiguration = configurationParser
                    .fromJson(widgetConfiguration, AppWidgetConfiguration::class.java)
                // 保存应用小组件配置信息
                applicationContext.configurationStore.edit {
                    it[WIDGET_CONFIGURATION_KEY] = widgetConfiguration
                }
                // 注册应用小组件服务
                AppWidgetService.registerWidgetService(appWidgetConfiguration!!)
                // 注册场景小组件的配置信息
                SceneWidgetController.registerSceneConfiguration(
                    context = applicationContext,
                    widgetConfiguration = appWidgetConfiguration!!
                )
                // 注册控制小组件的配置信息
                ControlWidgetController.registerControlConfiguration(
                    context = applicationContext,
                    widgetConfiguration = appWidgetConfiguration!!
                )
                // 注册状态小组件的配置信息
                StateWidgetController.registerStateConfiguration(
                    context = applicationContext,
                    widgetConfiguration = appWidgetConfiguration!!
                )
            } catch (e: Exception) {
                // 发生未知错误
                e.printStackTrace()
            }
        }
    }

    /**
     * 注册场景小组件的配置信息
     * @param configuration 场景小组件的配置信息
     */
    fun registerSceneConfiguration(configuration: String) {
        if (appWidgetConfiguration == null) {
            return
        }
        SceneWidgetController.registerSceneConfiguration(
            context = applicationContext,
            widgetConfiguration = appWidgetConfiguration!!,
            configuration = configuration
        )
    }

    fun getSceneConfiguration(context: Context, callback: (String) -> Unit) =
        SceneWidgetController.getSceneConfiguration(context, callback)

    /**
     * 注册控制小组件的配置信息
     * @param configuration 控制小组件的配置信息
     */
    fun registerControlConfiguration(configuration: String) {
        if (appWidgetConfiguration == null) {
            return
        }
        ControlWidgetController.registerControlConfiguration(
            context = applicationContext,
            widgetConfiguration = appWidgetConfiguration!!,
            configuration = configuration
        )
    }

    fun getControlConfiguration(context: Context, callback: (String) -> Unit) =
        ControlWidgetController.getControlConfiguration(context, callback)

    /**
     * 注册状态小组件的配置想你想
     * @param configuration 状态小组件的配置信息
     */
    fun registerStateConfiguration(configuration: String) {
        if (appWidgetConfiguration == null) {
            return
        }
        StateWidgetController.registerStateConfiguration(
            context = applicationContext,
            widgetConfiguration = appWidgetConfiguration!!,
            configuration = configuration
        )
    }

    fun getStateConfiguration(context: Context, callback: (String) -> Unit) =
        StateWidgetController.getStateConfiguration(context, callback)

}








