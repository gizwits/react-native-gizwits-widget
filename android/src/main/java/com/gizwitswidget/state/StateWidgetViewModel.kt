package com.gizwitswidget.state

import android.app.Application
import com.gizwitswidget.AppWidgetWebSocketService
import com.gizwitswidget.WidgetViewModel
import com.gizwitswidget.control.ControlWidgetUiState
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepository
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepositoryImpl
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.model.StateConfiguration
import com.gizwitswidget.model.StateContent
import com.gizwitswidget.model.StateTitle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StateWidgetViewModel(application: Application) : WidgetViewModel(application) {

    /**
     * 用于解析传入的小组件相关配置信息的解析器
     */
    private val configurationParser: Gson = GsonBuilder().create()

    /**
     * 小组件配置文件存储库，用于更新获取对应应用小组件的配置文件
     */
    private val widgetConfigurationRepository: WidgetConfigurationRepository =
        WidgetConfigurationRepositoryImpl.getInstance()

    /**
     * 控制小组件使能状态
     */
    private val isStateWidgetEnabled: Flow<Boolean> = widgetConfigurationRepository
        .getStateWidgetEnabled(application)

    /**
     * 小组件的通用配置文件
     */
    private val commonConfiguration: Flow<AppWidgetConfiguration?> =
        widgetConfigurationRepository
            .getCommonWidgetConfiguration(application)
            .map { configuration ->
                if (configuration != null) {
                    configurationParser.fromJson(
                        configuration,
                        AppWidgetConfiguration::class.java
                    )
                } else {
                    null
                }
            }

    /**
     * 状态小组件的配置文件
     */
    private val stateConfigurations: Flow<List<StateConfiguration>> =
        widgetConfigurationRepository
            .getStateWidgetConfiguration(application)
            .map { configuration ->
                if (configuration != null) {
                    val collectionType: TypeToken<List<StateConfiguration>> =
                        object : TypeToken<List<StateConfiguration>>() {}
                    configurationParser.fromJson(configuration, collectionType.type)
                } else {
                    listOf()
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val subscribedDeviceInfos: Flow<List<SubscribedDeviceInfo>> =
        callbackFlow {
            // 初始化已订阅设备信息列表
            val subscribedDeviceInfoList: MutableList<SubscribedDeviceInfo> = mutableListOf()
            send(subscribedDeviceInfoList.toList())
            // 开始连接WebSocket服务
            AppWidgetWebSocketService.connect(application)
            // 监听设备连接以及状态信息
            val connectionStateListener: (String, Boolean) -> Unit = {
                    deviceId, isConnectd ->
                // 更新设备的连接状态
                synchronized(subscribedDeviceInfoList) {
                    var deviceInfo: SubscribedDeviceInfo? = subscribedDeviceInfoList
                        .find { it.deviceId == deviceId }
                    if (deviceInfo == null) {
                        deviceInfo = SubscribedDeviceInfo(deviceId)
                        subscribedDeviceInfoList.add(deviceInfo)
                    }
                    deviceInfo.isOnline = isConnectd
                }
                // 发布变更
                trySend(subscribedDeviceInfoList.toList())
            }
            AppWidgetWebSocketService.subscribeConnectionState(connectionStateListener)
            val deviceStateListener: (String, JsonObject) -> Unit = {
                    deviceId, attributes ->
                // 更新设备的状态信息
                synchronized(subscribedDeviceInfoList) {
                    var deviceInfo: SubscribedDeviceInfo? = subscribedDeviceInfoList
                        .find { it.deviceId == deviceId }
                    if (deviceInfo == null) {
                        deviceInfo = SubscribedDeviceInfo(deviceId)
                        subscribedDeviceInfoList.add(deviceInfo!!)
                    }
                    deviceInfo!!.attributes = attributes
                }
                // 发布变更
                trySend(subscribedDeviceInfoList.toList())
            }
            AppWidgetWebSocketService.subscribeDeviceState(deviceStateListener)
            awaitClose {
                // 取消连接、监听WebSocket服务
                viewModelScope.launch {
                    AppWidgetWebSocketService.unsubscribeConnectionState(connectionStateListener)
                    AppWidgetWebSocketService.unsubscribeDeviceState(deviceStateListener)
                    AppWidgetWebSocketService.disconnect()
                }
            }
        }

    private val _stateWidgetUiState: StateFlow<StateWidgetUiState> =
        isStateWidgetEnabled.transformLatest { isEnabled ->
            if (isEnabled) {
                val stateWidgetUiState: Flow<StateWidgetUiState> = combineLatest(
                    commonConfiguration,
                    stateConfigurations,
                    subscribedDeviceInfos
                ) { _commonConfiguration, _stateConfigurations, _subscribedDeviceInfos ->
                    if (_commonConfiguration == null) {
                        // 通用配置为空，直接返回
                        return@combineLatest StateWidgetUiState.Idle
                    }
                    StateWidgetUiState.Success(
                        itemStateList = _stateConfigurations.map { stateConfiguration ->
                            val subscribedDeviceInfo: SubscribedDeviceInfo? = _subscribedDeviceInfos
                                .find { it.deviceId == stateConfiguration.deviceId }
                            val attrsValue: JsonElement? = subscribedDeviceInfo?.attributes?.getAsJsonPrimitive(stateConfiguration.attrsKey)
                            StateWidgetItemState(
                                language = stateConfiguration.language,
                                languageKey = _commonConfiguration.languageKey,
                                nameId = stateConfiguration.nameId,
                                stateId = stateConfiguration.id,
                                icon = stateConfiguration.icon,
                                title = stateConfiguration.title,
                                deviceId = stateConfiguration.deviceId,
                                attrsKey = stateConfiguration.attrsKey,
                                attrsValue = attrsValue,
                                contentList = stateConfiguration.contentList,
                                isOnline = subscribedDeviceInfo?.isOnline ?: false
                            )
                        }
                    )
                }
                emitAll(stateWidgetUiState)
            } else {
                // 当前桌面无小组件，关闭小组件服务
                emit(StateWidgetUiState.Idle)
            }
        }.onEach {
            StateWidgetProvider.updateAppWidget(application)
            StateWidgetProvider.notifyAppWidgetViewDataChanged(application)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = StateWidgetUiState.Idle
        )

    val stateWidgetUiState: StateWidgetUiState
        get() = _stateWidgetUiState.value

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            widgetConfigurationRepository.setStateWidgetEnabled(
                context = application,
                enabled = enabled
            )
        }
    }

}

sealed interface StateWidgetUiState {

    /* 空闲状态，未加载任何小组件数据 */
    object Idle : StateWidgetUiState

    data class Success(
        val itemStateList: List<StateWidgetItemState>
    ) : StateWidgetUiState

}

data class StateWidgetItemState(
    val language: JsonObject,
    val languageKey: String,
    val nameId: String,
    val stateId: Int,
    val icon: String?,
    val title: StateTitle?,
    val deviceId: String,
    val attrsKey: String,
    val attrsValue: JsonElement?,
    val contentList: List<StateContent>,
    var isOnline: Boolean
)

private data class SubscribedDeviceInfo(
    val deviceId: String,
    var isOnline: Boolean? = null,
    var attributes: JsonObject? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
public fun <T1, T2, T3, R> combineLatest(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    transform: suspend (a: T1, b: T2, c: T3) -> R
): Flow<R> {
    return combine(flow1, flow2, flow3) { value1, value2, value3 ->
        Triple(value1, value2, value3)
    }.mapLatest {
        transform(it.first, it.second, it.third)
    }
}

