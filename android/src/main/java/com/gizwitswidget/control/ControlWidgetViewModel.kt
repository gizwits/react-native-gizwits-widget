package com.gizwitswidget.control

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.gizwitswidget.AppWidgetController
import com.gizwitswidget.AppWidgetWebSocketService
import com.gizwitswidget.R
import com.gizwitswidget.WidgetViewModel
import com.gizwitswidget.core.data.repository.WidgetApiRepository
import com.gizwitswidget.core.data.repository.WidgetApiRepositoryImpl
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepository
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepositoryImpl
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.model.ControlConfiguration
import com.gizwitswidget.model.ControlOption
import com.gizwitswidget.network.UserDeviceList
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class ControlWidgetViewModel(application: Application) : WidgetViewModel(application) {

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
    private val isControlWidgetEnabled: Flow<Boolean> = widgetConfigurationRepository
        .getControlWidgetEnabled(application)

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
     * 控制小组件的配置文件
     */
    private val controlConfigurations: Flow<List<ControlConfiguration>> =
        widgetConfigurationRepository
            .getControlWidgetConfiguration(application)
            .map { configuration ->
                if (configuration !=null) {
                    val collectionType: TypeToken<List<ControlConfiguration>> =
                        object : TypeToken<List<ControlConfiguration>>() {}
                    configurationParser.fromJson(configuration, collectionType.type)
                } else {
                    listOf()
                }
            }

    /**
     * 控制小组件设备的绑定信息
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
            // 等待获取应用小组件的全局配置文件
            val commonConfigurationStr: String = widgetConfigurationRepository
                .getCommonWidgetConfiguration(application)
                .mapNotNull { it }
                .first()
            val commonConfiguration: AppWidgetConfiguration = configurationParser
                .fromJson(commonConfigurationStr, AppWidgetConfiguration::class.java)
            // 开始连接WebSocket服务
            AppWidgetWebSocketService.connect(application, commonConfiguration)
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

    /**
     * 被控设备的状态信息列表
     */
    private val controlledDeviceInfoList: MutableList<ControlledDeviceInfo> = mutableListOf()

    /**
     * 被控设备的id、动作id以及控制设备任务对象的映射集合
     */
    private val controlledDeviceJobMap: MutableMap<String, Job> = mutableMapOf()

    /**
     * 被控设备的状态信息
     */
    private val controlledDeviceInfos: MutableSharedFlow<List<ControlledDeviceInfo>> =
        MutableSharedFlow<List<ControlledDeviceInfo>>(replay = 1).apply { tryEmit(listOf()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _controlWidgetUiState : StateFlow<ControlWidgetUiState> =
        isControlWidgetEnabled.transformLatest { isEnabled ->
            if (isEnabled) {
                // 当前桌面存在小组件，开启小组件服务
                val controlWidgetUiState: Flow<ControlWidgetUiState> = combineLatest(
                    commonConfiguration,
                    controlConfigurations,
                    boundDeviceInfos,
                    subscribedDeviceInfos,
                    controlledDeviceInfos
                ) { _commonConfiguration,
                    _controlConfigurations,
                    _boundDeviceInfos,
                    _subscribedDeviceInfos,
                    _controlledDeviceInfos ->

                    if (_commonConfiguration == null) {
                        return@combineLatest ControlWidgetUiState.Idle
                    }
                    // 通过控制配置信息和绑定设备信息，构建控制小组件列表项状态
                    val itemStateList: MutableList<ControlWidgetItemState> = mutableListOf()
                    _controlConfigurations.forEach { deviceInfo ->
                        // 获取对应绑定设备信息以及订阅信息、控制信息
                        val boundDeviceInfo: BoundDeviceInfo? = _boundDeviceInfos
                            .find { it.deviceId == deviceInfo.deviceId }
                        val subscribedDeviceInfo: SubscribedDeviceInfo? = _subscribedDeviceInfos
                            .find { it.deviceId == deviceInfo.deviceId }
                        // 获取设备的发布状态信息
                        val isSandbox: Boolean? = boundDeviceInfo?.isSandbox
                        val isOnline: Boolean = if (isSandbox != null && subscribedDeviceInfo != null) {
                            subscribedDeviceInfo.isOnline ?: false
                        } else {
                            false
                        }

                        // 迭代控制组件的属性配置项，构建列表项的Ui状态
                        deviceInfo.configs.forEach configsLoop@{ config ->
                            val controlledDeviceInfo: ControlledDeviceInfo? = _controlledDeviceInfos
                                .find { it.deviceId == deviceInfo.deviceId && it.controlId.toString() == config.id}

                            val deviceName: String? = controlledDeviceInfo?.deviceName ?: deviceInfo.name ?: boundDeviceInfo?.deviceName
                            if (deviceName == null) {
                                // 如果设备名称为空，则不添加至设备列表
                                return@configsLoop
                            }

                            val attrsValue: JsonElement? = controlledDeviceInfo?.attrsValue
                                ?: subscribedDeviceInfo?.attributes?.getAsJsonPrimitive(config.attrsKey)
                            itemStateList.add(
                                ControlWidgetItemState(
                                    nameId = config.nameId,
                                    configId = config.id,
                                    tintColor = _commonConfiguration.tintColor,
                                    languageKey = _commonConfiguration.languageKey,
                                    language = deviceInfo.language,
                                    deviceId = deviceInfo.deviceId,
                                    deviceName = deviceName,
                                    deviceIcon = deviceInfo.icon,
                                    attrsType = config.type,
                                    attrsIcon = config.attrsIcon,
                                    attrsKey = config.attrsKey,
                                    attrsValue = attrsValue,
                                    options = config.options,
                                    isOnline = isOnline,
                                    isSandbox = isSandbox
                                )
                            )
                        }
                    }
                    // 构建控制小组件Ui状态
                    ControlWidgetUiState.Success(
                        itemStateList = itemStateList
                    )
                }
                emitAll(controlWidgetUiState)
            } else {
                // 当前桌面无小组件，关闭小组件服务
                emit(ControlWidgetUiState.Idle)
            }
        }.onStart {
            // 通知小组件更新视图
            ControlWidgetProvider.updateAppWidget(application)
        }.onEach {
            // 通知小组件列表更新视图
            ControlWidgetProvider.notifyAppWidgetViewDataChanged(application)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ControlWidgetUiState.Idle
        )

    val controlWidgetUiState: ControlWidgetUiState
        get() = _controlWidgetUiState.value

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            widgetConfigurationRepository.setControlWidgetEnabled(
                context = application,
                enabled = enabled
            )
        }
    }

    /**
     * 执行指定控制动作
     * @param deviceId 被控设备的id
     * @param controlId 控制动作的id
     * @param isSandBox 被控设备的发布状态
     */
    fun executeControl(deviceId: String, controlId: Int, isSandBox: Boolean?) {
        val controlWidgetUiState: ControlWidgetUiState = this.controlWidgetUiState
        if (controlWidgetUiState !is ControlWidgetUiState.Success) {
            // 当前状态不符合，直接退出
            return
        }
        // 检索被控子项的状态，不存在则直接退出
        val controlWidgetItemState: ControlWidgetItemState = controlWidgetUiState
            .itemStateList.find { it.deviceId == deviceId && it.configId == controlId.toString() } ?: return
        // 检索当前对应设备和控制动作的控制任务
        val controlledDeviceJobKey: String = deviceId + controlId
        val lastControlledDeviceJob: Job? = controlledDeviceJobMap[controlledDeviceJobKey]
        val newControlledDeviceJob: Job = viewModelScope.launch {
            // 取消执行上一次任务
            lastControlledDeviceJob?.cancelAndJoin()
            // 开始执行本次任务
            val controlledDeviceInfo: ControlledDeviceInfo = ControlledDeviceInfo(deviceId, controlId)
            try {
                val isOnline: Boolean = controlWidgetItemState.isOnline     // 设备在线状态
                if (!isOnline || isSandBox == null) {
                    // 当前设备不在线，则标题显示离线两秒然后复原
                    controlledDeviceInfo.deviceName = application.getString(R.string.offline)
                    controlledDeviceInfoList.add(controlledDeviceInfo)
                    controlledDeviceInfos.emit(controlledDeviceInfoList.toList())
                    delay(2000)
                } else {
                    // 当前设备在线，则直接执行动作
                    // 根据当前状态获取下一项控制的状态
                    val attrsType: String = controlWidgetItemState.attrsType
                    val attrsKey: String = controlWidgetItemState.attrsKey
                    val attrsValue: JsonElement? = controlWidgetItemState.attrsValue
                    val options: List<ControlOption> = controlWidgetItemState.options
                    val newAttrsValue: JsonElement = when (attrsType) {
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
                                    options.indexOfFirst {
                                        it.value == attrsValue
                                    }.let {
                                        // 获取旧值的索引并加1，如果新索引在选项数组范围内则应用，否则重置为0
                                        val index: Int = it + 1
                                        if (index in 0 until options.size) {
                                            index
                                        } else {
                                            0
                                        }
                                    }
                                }
                            }
                            val newSttrsValue = options[newIndex].value
                            newSttrsValue
                        }
                    }
                    // 预先更新设备状态至预定值，失败则回滚
                    controlledDeviceInfo.attrsValue = newAttrsValue
                    controlledDeviceInfoList.add(controlledDeviceInfo)
                    controlledDeviceInfos.emit(controlledDeviceInfoList.toList())
                    // 构建并发送任务执行请求
                    val controlAttributesBody: JsonObject = JsonObject().apply {
                        add(attrsKey, newAttrsValue)
                    }
                    AppWidgetWebSocketService.executeControl(
                        deviceId = deviceId,
                        attributes = controlAttributesBody,
                        isSandBox = isSandBox
                    )
                    delay(2000)
                }
            } finally {
                // 任务执行完成移除所有任务效果
                withContext(NonCancellable) {
                    controlledDeviceInfoList.remove(controlledDeviceInfo)
                    controlledDeviceInfos.emit(controlledDeviceInfoList.toList())
                }
            }
        }
        controlledDeviceJobMap[controlledDeviceJobKey] = newControlledDeviceJob
    }

}

sealed interface ControlWidgetUiState {

    /* 空闲状态，未加载任何小组件数据 */
    object Idle : ControlWidgetUiState

    /* 已加载状态，显示控制小组件设备列表 */
    data class Success(
        val itemStateList: List<ControlWidgetItemState>
    ) : ControlWidgetUiState

}

data class ControlWidgetItemState(
    val nameId: String,
    val configId: String,
    val tintColor: String,
    val languageKey: String,
    val language: JsonObject,
    val deviceId: String,
    var deviceName: String,
    val deviceIcon: String,
    val attrsType: String,
    val attrsIcon: String,
    val attrsKey: String,
    val attrsValue: JsonElement?,
    val options: List<ControlOption>,
    var isOnline: Boolean,
    val isSandbox: Boolean?
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

private data class ControlledDeviceInfo(
    val deviceId: String,
    val controlId: Int,
    var deviceName: String? = null,
    var attrsValue: JsonElement? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
fun <T1, T2, R> combineLatest(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    transform: suspend (a: T1, b: T2) -> R
): Flow<R> {
    return combine(flow1, flow2) { value1, value2 ->
        value1 to value2
    }.mapLatest {
        transform(it.first, it.second)
    }
}

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

@OptIn(ExperimentalCoroutinesApi::class)
public fun <T1, T2, T3, T4, T5, R> combineLatest(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    transform: suspend (a: T1, b: T2, c: T3, d: T4, e: T5) -> R
): Flow<R> {
    return combine(flow1, flow2, flow3, flow4, flow5) { value1, value2, value3, value4, value5 ->
        listOf(value1, value2, value3 ,value4, value5)
    }.mapLatest {
        transform(it[0] as T1, it[1] as T2, it[2] as T3, it[3] as T4, it[4] as T5)
    }
}


