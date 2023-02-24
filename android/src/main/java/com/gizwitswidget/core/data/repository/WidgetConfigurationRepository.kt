package com.gizwitswidget.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface WidgetConfigurationRepository {

    /**
     * 设置更新通用的小组件配置信息
     * @param context 上下文对象
     * @param configuration 配置信息
     */
    suspend fun setCommonWidgetConfiguration(context: Context, configuration: String)

    /**
     * 获取通用的小组件配置信息
     * @param context 上下文对象
     * @return 配置信息流
     */
    fun getCommonWidgetConfiguration(context: Context): Flow<String?>

    /**
     * 设置场景小组件的使能信息
     * @param enabled 是否开启控制小组件
     */
    suspend fun setSceneWidgetEnabled(context: Context, enabled: Boolean)

    /**
     * 获取场景小组件的使能信息
     * @return 控制小组件的使能信息流
     */
    fun getSceneWidgetEnabled(context: Context): Flow<Boolean>

    /**
     * 设置控制小组件的使能信息
     * @param enabled 是否开启控制小组件
     */
    suspend fun setControlWidgetEnabled(context: Context, enabled: Boolean)

    /**
     * 获取控制小组件的使能信息
     * @return 控制小组件的使能信息流
     */
    fun getControlWidgetEnabled(context: Context): Flow<Boolean>

    /**
     * 设置状态小组件的使能信息
     * @param enabled 是否开启控制小组件
     */
    suspend fun setStateWidgetEnabled(context: Context, enabled: Boolean)

    /**
     * 获取状态小组件的使能信息
     * @return 控制小组件的使能信息流
     */
    fun getStateWidgetEnabled(context: Context): Flow<Boolean>

    /**
     * 设置更新场景小组件的配置信息
     * @param context 上下文对象
     * @param configuration 场景小组件的配置信息
     */
    suspend fun setSceneWidgetConfiguration(context: Context, configuration: String)

    /**
     * 获取场景小组件的配置信息，如果当前配置不存在，则返回空字符串
     * @param context 上下文对象
     * @return 场景小组件的配置信息流
     */
    fun getSceneWidgetConfiguration(context: Context): Flow<String?>

    /**
     * 设置更新控制小组件的配置信息
     * @param context 上下文对象
     * @param configuration 控制小组件的配置信息
     */
    suspend fun setControlWidgetConfiguration(context: Context, configuration: String)

    /**
     * 获取控制小组件的配置信息，如果当前配置不存在，则返回空字符串
     * @param context 上下文对象
     * @return 控制小组件的配置信息流
     */
    fun getControlWidgetConfiguration(context: Context): Flow<String?>

    /**
     * 设置更新状态小组件的配置信息
     * @param context 上下文对象
     * @param configuration 控制小组件的配置信息
     */
    suspend fun setStateWidgetConfiguration(context: Context, configuration: String)

    /**
     * 获取状态小组件的配置信息，如果当前配置不存在，则返回空字符串
     * @param context 上下文对象
     * @return 控制小组件的配置信息流
     */
    fun getStateWidgetConfiguration(context: Context): Flow<String?>

}

private val Context.widgetConfigurationStore: DataStore<Preferences>
    by preferencesDataStore(name = "widget_configuration")

class WidgetConfigurationRepositoryImpl private constructor()
    : WidgetConfigurationRepository {

    private val COMMON_CONFIGURATION_KEY = stringPreferencesKey("common_configuration")

    private val IS_SCENE_ENABLED_KEY = booleanPreferencesKey("is_scene_enabled")

    private val IS_CONTROL_ENABLED_KEY = booleanPreferencesKey("is_control_enabled")

    private val IS_STATE_ENABLED_KEY = booleanPreferencesKey("is_state_enabled")

    private val SCENE_CONFIGURATION_KEY = stringPreferencesKey("scene_configuration")

    private val CONTROL_CONFIGURATION_KEY = stringPreferencesKey("control_configuration")

    private val STATE_CONFIGURATION_KEY = stringPreferencesKey("state_configuration")

    override suspend fun setCommonWidgetConfiguration(context: Context, configuration: String) {
        context.widgetConfigurationStore.edit {
            it[COMMON_CONFIGURATION_KEY] = configuration
        }
    }

    override fun getCommonWidgetConfiguration(context: Context): Flow<String?> {
        return context.widgetConfigurationStore.data
            .map {
                val commonConfiguration: String? = it[COMMON_CONFIGURATION_KEY]
                if (commonConfiguration.isNullOrEmpty()) {
                    null
                } else {
                    commonConfiguration
                }
            }
    }

    override suspend fun setSceneWidgetEnabled(context: Context, enabled: Boolean) {
        context.widgetConfigurationStore.edit {
            it[IS_SCENE_ENABLED_KEY] = enabled
        }
    }

    override fun getSceneWidgetEnabled(context: Context): Flow<Boolean> {
        return context.widgetConfigurationStore.data
            .map {
                it[IS_SCENE_ENABLED_KEY] ?: false
            }
    }

    override suspend fun setStateWidgetEnabled(context: Context, enabled: Boolean) {
        context.widgetConfigurationStore.edit {
            it[IS_STATE_ENABLED_KEY] = enabled
        }
    }

    override fun getStateWidgetEnabled(context: Context): Flow<Boolean> {
        return context.widgetConfigurationStore.data
            .map {
                it[IS_STATE_ENABLED_KEY] ?: false
            }
    }

    override suspend fun setSceneWidgetConfiguration(context: Context, configuration: String) {
        context.widgetConfigurationStore.edit {
            it[SCENE_CONFIGURATION_KEY] = configuration
        }
    }

    override fun getSceneWidgetConfiguration(context: Context): Flow<String?> {
        return context.widgetConfigurationStore.data
            .map {
                val sceneConfiguration: String? = it[SCENE_CONFIGURATION_KEY]
                if (sceneConfiguration.isNullOrEmpty()) {
                    null
                } else {
                    sceneConfiguration
                }
            }
    }

    override suspend fun setControlWidgetEnabled(context: Context, enabled: Boolean) {
        context.widgetConfigurationStore.edit {
            it[IS_CONTROL_ENABLED_KEY] = enabled
        }
    }

    override fun getControlWidgetEnabled(context: Context): Flow<Boolean> {
        return context.widgetConfigurationStore.data
            .map {
                it[IS_CONTROL_ENABLED_KEY] ?: false
            }
    }

    override suspend fun setControlWidgetConfiguration(context: Context, configuration: String) {
        context.widgetConfigurationStore.edit {
            it[CONTROL_CONFIGURATION_KEY] = configuration
        }
    }

    override fun getControlWidgetConfiguration(context: Context): Flow<String?> {
        return context.widgetConfigurationStore.data
            .map {
                val controlConfiguration: String? = it[CONTROL_CONFIGURATION_KEY]
                if (controlConfiguration.isNullOrEmpty()) {
                    null
                } else {
                    controlConfiguration
                }
            }
    }

    override suspend fun setStateWidgetConfiguration(context: Context, configuration: String) {
        context.widgetConfigurationStore.edit {
            it[STATE_CONFIGURATION_KEY] = configuration
        }
    }

    override fun getStateWidgetConfiguration(context: Context): Flow<String?> {
        return context.widgetConfigurationStore.data
            .map {
                val stateConfiguration: String? = it[STATE_CONFIGURATION_KEY]
                if (stateConfiguration.isNullOrEmpty()) {
                    null
                } else {
                    stateConfiguration
                }
            }
    }

    companion object {

        fun getInstance(): WidgetConfigurationRepository = WidgetConfigurationRepositoryImpl()

    }

}
