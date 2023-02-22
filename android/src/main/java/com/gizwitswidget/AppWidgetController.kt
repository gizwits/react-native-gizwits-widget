package com.gizwitswidget

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
     * 本地键值对存储的Key，用于存储当前小组件全局配置信息的有效性
     */
    private val IS_WIDGET_CONFIGURATION_VALID = booleanPreferencesKey("isWidgetConfigurationValid")

    /**
     * 标志位，记录当前小组件控制器是否被多次注册
     */
    @Volatile
    private var isRegistered: Boolean = false

    /**
     * 注册小组件控制器，此接口负责加载小组件服务以及加载已存储的小组件配置信息，
     * 此接口在应用的生命周期中只能调用一次
     * @param context 应用上下文对象
     */
    @Synchronized
    fun registerController(context: Context) {
        if (isRegistered) {
            throw RuntimeException("The widget controller has been registered")
        }
        applicationContext = context.applicationContext
        registerWidgetConfiguration()
        isRegistered = true
    }

    /**
     * 尝试注册小组件控制器，如果当前小组件控制器已注册，则此接口不会执行任何业务
     * @param context 应用上下文对象
     */
    @Synchronized
    fun tryRegisterController(context: Context) {
        if (isRegistered) {
            return
        }
        registerController(context)
    }

    /**
     * 注册应用小组件的全局通用配置信息，如果传入的配置信息为空，则加载已存储的配置信息，
     * 并启动应用小组件的服务
     * @param configuration 应用小组件的配置信息
     */
    fun registerWidgetConfiguration(configuration: String? = null) {
        // 启动协程初始化解析应用小组件的配置信息
        scope.launch {
            val widgetConfiguration: String = configuration ?: applicationContext
                .configurationStore.data.first()[WIDGET_CONFIGURATION_KEY] ?: return@launch
            var isWidgetConfigurationValid: Boolean = applicationContext
                .configurationStore.data.first()[IS_WIDGET_CONFIGURATION_VALID] ?: false
            if (configuration != null) {
                // 主动传入的全局配置信息不为空，则配置信息有效
                isWidgetConfigurationValid = true
                applicationContext.configurationStore.edit {
                    it[IS_WIDGET_CONFIGURATION_VALID] = true
                }
            }
            if (!isWidgetConfigurationValid) {
                // 应用小组件的全局配置信息无效，退出注册
                return@launch
            }
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
     * 注销应用小组件的全局通用配置信息
     */
    fun deregisterWidgetConfiguration() {
        // TODO 关闭断开应用小组件的服务连接
        scope.launch {
            // 无效化当前小组件的全局配置信息
            applicationContext.configurationStore.edit {
                it[IS_WIDGET_CONFIGURATION_VALID] = false
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

    /**
     * 获取应用场景小组件的配置信息
     * @param context 应用上下文对象
     * @param callback 获取应用场景小组件配置的回调接口
     */
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

    /**
     * 获取应用控制小组件的配置信息
     * @param context 应用上下文对象
     * @param callback 获取应用控制小组件配置的回调接口
     */
    fun getControlConfiguration(context: Context, callback: (String) -> Unit) =
        ControlWidgetController.getControlConfiguration(context, callback)

    /**
     * 注册状态小组件的配置信息
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

    /**
     * 获取状态小组件的配置信息
     * @param context 应用上下文对象
     * @param callback 获取应用状态小组件配置的回调接口
     */
    fun getStateConfiguration(context: Context, callback: (String) -> Unit) =
        StateWidgetController.getStateConfiguration(context, callback)

}








