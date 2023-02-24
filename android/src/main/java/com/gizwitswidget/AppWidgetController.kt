package com.gizwitswidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.gizwitswidget.control.ControlWidgetView
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepository
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepositoryImpl
import com.gizwitswidget.scene.SceneWidgetView
import com.gizwitswidget.state.StateWidgetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AppWidgetController {

    private const val TAG: String = "AppWidgetController"

    /**
     * 关联应用小组件控制器的协程作用域
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * 小组件的配置存储库
     */
    private val widgetConfigurationRepository: WidgetConfigurationRepository =
        WidgetConfigurationRepositoryImpl.getInstance()

    /**
     * 激活小组件的实现
     */
    fun activateAppWidget(context: Context) {
        SceneWidgetView.onCreate(context)
        ControlWidgetView.onCreate(context)
        StateWidgetView.onCreate(context)
    }

    /**
     * 设置更新通用的小组件配置信息
     * @param context 上下文对象
     * @param configuration 配置信息
     */
    fun setCommonWidgetConfiguration(context: Context, configuration: String) {
        scope.launch {
            widgetConfigurationRepository.setCommonWidgetConfiguration(context, configuration)
            activateAppWidget(context)
        }
    }

    /**
     * 设置更新场景小组件的配置信息
     * @param context 上下文对象
     * @param configuration 场景小组件的配置信息
     */
    fun setSceneWidgetConfiguration(context: Context, configuration: String) {
        scope.launch {
            widgetConfigurationRepository.setSceneWidgetConfiguration(context, configuration)
        }
    }

    /**
     * 获取场景小组件的配置信息
     * @param context 上下文对象
     * @param callback 获取配置信息回调
     */
    fun getSceneWidgetConfiguration(context: Context, callback: (String) -> Unit) {
        scope.launch {
            val configuration: String = widgetConfigurationRepository
                .getSceneWidgetConfiguration(context)
                .first() ?: ""
            callback(configuration)
        }
    }

    /**
     * 设置更新控制小组件的配置信息
     * @param configuration 控制小组件的配置信息
     */
    fun setControlWidgetConfiguration(context: Context, configuration: String) {
        scope.launch {
            widgetConfigurationRepository.setControlWidgetConfiguration(context, configuration)
        }
    }

    /**
     * 获取控制小组件的配置信息
     * @param context 上下文对象
     * @param callback 获取配置信息回调
     */
    fun getControlWidgetConfiguration(context: Context, callback: (String) -> Unit) {
        scope.launch {
            val configuration: String = widgetConfigurationRepository
                .getControlWidgetConfiguration(context)
                .first() ?: ""
            callback(configuration)
        }
    }

    /**
     * 设置更新状态小组件的配置信息
     * @param configuration 状态小组件的配置信息
     */
    fun setStateWidgetConfiguration(context: Context, configuration: String) {
        scope.launch {
            widgetConfigurationRepository.setStateWidgetConfiguration(context, configuration)
        }
    }

    /**
     * 获取状态小组件的配置信息
     * @param context 上下文对象
     * @param callback 获取配置信息回调
     */
    fun getStateWidgetConfiguration(context: Context, callback: (String) -> Unit) {
        scope.launch {
            val configuration: String = widgetConfigurationRepository
                .getStateWidgetConfiguration(context)
                .first() ?: ""
            callback(configuration)
        }
    }

    fun clearWidgetConfiguration(context: Context) {
        scope.launch {
            widgetConfigurationRepository.setSceneWidgetConfiguration(context, "")
            widgetConfigurationRepository.setControlWidgetConfiguration(context, "")
            widgetConfigurationRepository.setStateWidgetConfiguration(context, "")
        }
    }

}








