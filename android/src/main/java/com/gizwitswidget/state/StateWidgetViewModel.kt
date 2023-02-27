package com.gizwitswidget.state

import android.app.Application
import com.gizwitswidget.AppWidgetWebSocketService
import com.gizwitswidget.WidgetViewModel
import com.gizwitswidget.core.data.repository.WidgetApiRepository
import com.gizwitswidget.core.data.repository.WidgetApiRepositoryImpl
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepository
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepositoryImpl
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.model.StateConfiguration
import com.gizwitswidget.model.StateContent
import com.gizwitswidget.model.StateTitle
import com.gizwitswidget.network.UserDeviceList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

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

    /**
     * 状态小组件设备的绑定信息
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val boundDeviceInfos: Flow<List<BoundDeviceInfo>> =
        commonConfiguration.transformLatest { commonConfig ->
            emit(listOf<BoundDeviceInfo>())
            if (commonConfig == null) {
                // 通用的小组件配置信息为空，直接退出
                return@transformLatest
            }
            val widgetApiRepository: WidgetApiRepository = WidgetApiRepositoryImpl(
                openUrl = commonConfig.openUrl,
                aepUrl = commonConfig.aepUrl,
                appId = commonConfig.appId,
                userToken = commonConfig.userToken
            )
            while (currentCoroutineContext().isActive) {
                val result: Result<UserDeviceList> = widgetApiRepository.fetchUserDeviceList()
                if (result.isFailure || !result.getOrThrow().isSuccess()) {
                    // 请求设备列表信息失败，延时重试
                    delay(2000)
                    continue
                }
                emit(
                    result.getOrThrow().devices.map {
                        BoundDeviceInfo(
                            deviceId = it.deviceId,
                            deviceName = it.deviceName,
                            isOnline = it.isOnline,
                            isSandbox = it.isSandbox
                        )
                    }
                )
                return@transformLatest
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
                    boundDeviceInfos,
                    subscribedDeviceInfos
                ) { _commonConfiguration, _stateConfigurations, _boundDeviceInfos, _subscribedDeviceInfos ->
                    if (_commonConfiguration == null) {
                        // 通用配置为空，直接返回
                        return@combineLatest StateWidgetUiState.Idle
                    }
                    StateWidgetUiState.Success(
                        itemStateList = _stateConfigurations.mapNotNull { stateConfiguration ->
                            val boundDeviceInfo: BoundDeviceInfo? = _boundDeviceInfos
                                .find { it.deviceId == stateConfiguration.deviceId }
                            if (boundDeviceInfo == null) {
                                // 如果未获取到绑定设备列表，则不显示设备
                                return@mapNotNull null
                            }
                            val subscribedDeviceInfo: SubscribedDeviceInfo? = _subscribedDeviceInfos
                                .find { it.deviceId == stateConfiguration.deviceId }
                            val attrsValue: JsonElement? = subscribedDeviceInfo?.attributes?.getAsJsonPrimitive(stateConfiguration.attrsKey)
                            StateWidgetItemState(
                                language = stateConfiguration.language,
                                languageKey = _commonConfiguration.languageKey,
                                editName = stateConfiguration.editName,
                                stateId = stateConfiguration.id,
                                icon = stateConfiguration.icon,
                                title = stateConfiguration.title,
                                deviceName = boundDeviceInfo.deviceName,
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
        }.onStart {
            StateWidgetProvider.updateAppWidget(application)
        }.onEach {
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
    val editName: String,
    val stateId: Int,
    val icon: String?,
    val title: StateTitle?,
    val deviceName: String,
    val deviceId: String,
    val attrsKey: String,
    val attrsValue: JsonElement?,
    val contentList: List<StateContent>,
    var isOnline: Boolean
)

private data class BoundDeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val isOnline: Boolean,
    val isSandbox: Boolean
)

private data class SubscribedDeviceInfo(
    val deviceId: String,
    var isOnline: Boolean? = null,
    var attributes: JsonObject? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
public fun <T1, T2, T3, T4, R> combineLatest(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    transform: suspend (a: T1, b: T2, c: T3, d: T4) -> R
): Flow<R> {
    return combine(flow1, flow2, flow3, flow4) { value1, value2, value3, value4 ->
        listOf(value1, value2, value3 ,value4)
    }.mapLatest {
        transform(it[0] as T1, it[1] as T2, it[2] as T3, it[3] as T4)
    }
}
