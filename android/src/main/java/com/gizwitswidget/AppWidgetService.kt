package com.gizwitswidget

import com.gizwitswidget.model.StateConfiguration
import com.gizwitswidget.network.OpenApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.model.ControlConfiguration
import com.gizwitswidget.network.HeaderManageInterceptor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppWidgetService : WebSocketListener() {

    private const val TAG: String = "AppWidgetService"

    /**
     * WebSocket服务相关状态码：正常关闭
     */
    private const val STATE_CODE_NORMAL_CLOSURE: Int = 1000

    /**
     * 指令：维持与服务连接的心跳包指令
     */
    private const val COMMAND_HEART_BEAT: String = "ping"

    /**
     * 指令：WebSocket服务用于发布设备连接状态的指令
     */
    private const val COMMAND_CONNECTION_STATE: String = "s2c_online_status"

    /**
     * 指令：用于请求登录WebSocket服务
     */
    private const val COMMAND_LOGIN_REQUEST: String = "login_req"

    /**
     * 指令：登录服务的响应指令
     */
    private const val COMMAND_LOGIN_RESPONSE: String = "login_res"

    /**
     * 指令：服务发布设备状态数据的指令
     */
    private const val COMMAND_DEVICE_DATA_STATE: String = "s2c_noti"

    /**
     * 指令：向服务请求设备状态数据的指令
     */
    private const val COMMAND_READ_DEVICE_DATA_REQUEST: String = "c2s_read"

    /**
     * 指令：向服务请求设备状态数据的响应指令
     */
    private const val COMMAND_READ_DEVICE_DATA_RESPONSE: String = COMMAND_DEVICE_DATA_STATE

    /**
     * 指令：向服务请求写入设备状态数据的指令
     */
    private const val COMMAND_WRITE_DEVICE_DATA_REQUEST: String = "c2s_write"

    /**
     * 指令：向服务请求写入设备状态数据的响应指令
     */
    private const val COMMAND_WRITE_DEVICE_DATA_RESPONSE: String = COMMAND_DEVICE_DATA_STATE

    /**
     * WebSocket服务响应数据解析器
     */
    private val parser: Gson = GsonBuilder().create()

    /**
     * 用于任务调度的协程作用域
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * WebSocket客户端
     */
    private val webSocketClient: OkHttpClient = OkHttpClient.Builder().build()

    /**
     * WebSocket连接对象
     */
    private lateinit var webSocket: WebSocket

    /**
     * OpenApi接口对象
     */
    private lateinit var openApi: OpenApi

    /**
     * 应用小组件配置信息，为空则表示从未进行过注册初始化[registerWidgetService]
     */
    private lateinit var appWidgetConfiguration: AppWidgetConfiguration

    /**
     * 控制小组件的配置信息
     */
    private var controlConfigurationList: List<ControlConfiguration> = emptyList()

    /**
     * 控制小组件控制器的设备连接变更事件回调对象
     */
    private var controlConnectionChangedCallback: (String, Boolean) -> Unit = { _, _ ->

    }

    /**
     * 控制小组件的设备信息变更事件回调对象
     */
    private var controlDeviceInformationChangedCallback: (String, String) -> Unit = { _, _ ->

    }

    /**
     * 控制小组件控制器的设备数据状态变更事件回调对象
     */
    private var controlDataStateChangedCallback: (String, JsonObject) -> Unit = { _, _ ->

    }

    /**
     * 状态小组件的配置信息
     */
    private var stateConfigurationList: List<StateConfiguration> = emptyList()

    /**
     * 状态小组件控制器的设备连接变更事件回调对象
     */
    private var stateConnectionChangedCallback: (String, Boolean) -> Unit = { _, _ ->

    }

    /**
     * 状态小组件控制器的设备数据状态变更事件回调对象
     */
    private var stateDataStateChangedCallback: (String, JsonObject) -> Unit = { _, _ ->

    }

    /**
     * 服务回调的消息数据流
     */
    private val responseFlow: MutableSharedFlow<WebSocketResponse> =
        MutableSharedFlow(extraBufferCapacity = 1)

    /**
     * WebSocket服务相关会话业务管理任务
     */
    private var sessionJob: Job? = null

    /**
     * 标志位，指示当前服务是否以及完成
     */
    private var isRegistered: Boolean = false

    /**
     * 标志位，指示当前是否处于连接WebSocket服务的状态（逻辑上），当处于[connectWebSocket]
     * 和[disconnectWebSocket]调用之间时，此值为true，与实际的连接状态无关。
     */
    private var isConnected: Boolean = false

    /**
     * 标志位，指示当前是否已经成功连接到WebSocket服务（[onOpen]被调用）
     */
    private var isOpen: Boolean = false

    /**
     * 注册初始化应用小组件服务
     * @param configuration 应用小组件配置
     */
    @Synchronized
    fun registerWidgetService(
        configuration: AppWidgetConfiguration
    ) {
        // 使用新的应用小组件配置信息连接WebSocket服务
        connectWebSocket(configuration)
        // 初始化依赖对象
        openApi = Retrofit.Builder()
            .baseUrl(appWidgetConfiguration.openUrl)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HeaderManageInterceptor(appWidgetConfiguration.appId, appWidgetConfiguration.userToken)
                    ).build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(OpenApi::class.java)
        isRegistered = true
    }

    /**
     * 注册初始化控制小组件服务
     * @param controlConfigurations 控制小组件配置信息
     * @param onDeviceConnectionChanged 设备连接状态变更回调
     * @param onDeviceInformationChanged 设备信息变更回调
     * @param onDeviceStateChanged 设备数据点状态变更回调
     */
    fun registerControlService(
        controlConfigurations: List<ControlConfiguration>,
        onDeviceConnectionChanged: (String, Boolean) -> Unit,
        onDeviceInformationChanged: (String, String) -> Unit,
        onDeviceStateChanged: (String, JsonObject) -> Unit
    ) {
        controlConfigurationList = controlConfigurations
        controlConnectionChangedCallback = onDeviceConnectionChanged
        controlDeviceInformationChangedCallback = onDeviceInformationChanged
        controlDataStateChangedCallback = onDeviceStateChanged
        // 更新用户绑定设备列表
        updateUserDeviceList()
        // 更新控制小组件的设备列表中的设备状态
        updateControlDeviceListState()
    }

    /**
     * 注册初始化状态小组件服务
     * @param stateConfigurations 状态小组件配置信息
     * @param onDeviceConnectionChanged 设备连接状态变更回调
     * @param onDeviceStateChanged 设备数据点状态变更回调
     */
    fun registerStateService(
        stateConfigurations: List<StateConfiguration>,
        onDeviceConnectionChanged: (String, Boolean) -> Unit,
        onDeviceStateChanged: (String, JsonObject) -> Unit
    ) {
        stateConfigurationList = stateConfigurations
        stateConnectionChangedCallback = onDeviceConnectionChanged
        stateDataStateChangedCallback = onDeviceStateChanged
        // 更新用户绑定设备列表
        updateUserDeviceList()
        // 更新状态小组件的设备列表中的设备状态
        updateStateDeviceListState()
    }

    /**
     * 执行设备数据点控制
     * @param deviceId 设备ID
     * @param attributes 写入控制的属性字段信息
     */
    fun executeControl(deviceId: String, attributes: JsonObject) {
        val controlCommandBody: JsonObject = JsonObject().apply {
            addProperty("did", deviceId)
            add("attrs", attributes)
        }
        sendCommandRequest(COMMAND_WRITE_DEVICE_DATA_REQUEST, controlCommandBody)
    }

    /**
     * 使用指定的小组件配置信息连接WebSocket服务，如果当前服务已连接并且配置信息相等，
     * 则忽略此请求，否则创建新连接。
     * @param configuration 应用小组件配置信息
     */
    @Synchronized
    private fun connectWebSocket(configuration: AppWidgetConfiguration) {
        // 根据配置信息判断当前服务的状态是否需要重连，需要则进行重连
        if (isConnected) {
            // 处于连接状态，则小组件配置信息肯定不为空
            val oldConfiguration: AppWidgetConfiguration = appWidgetConfiguration
            if (oldConfiguration.appId != configuration.appId ||
                oldConfiguration.userId != configuration.userId ||
                oldConfiguration.userToken != configuration.userToken ||
                oldConfiguration.m2mUrl != configuration.m2mUrl) {
                // 配置发生改变，断开连接进行重连
                disconnectWebSocket()
            } else {
                // 配置未发生改变，无动作
                return
            }
        }
        // 当服务未连接或者服务连接配置信息更新时，则进行连接
        appWidgetConfiguration = configuration
        val request: Request = Request.Builder()
            .url(appWidgetConfiguration.m2mUrl)      // 实际修改为生产环境
            .build()
        webSocket = webSocketClient.newWebSocket(request, this)
        isConnected = true
    }

    @Synchronized
    private fun reconnectWebSocket(oldWebSocket: WebSocket) {
        if (webSocket != oldWebSocket) {
            return
        }
        webSocket.close(STATE_CODE_NORMAL_CLOSURE, null)
        val request: Request = Request.Builder()
            .url(appWidgetConfiguration.m2mUrl)      // 实际修改为生产环境
            .build()
        webSocket = webSocketClient.newWebSocket(request, this)
    }

    /**
     * 断开与WebSocket服务的连接
     */
    @Synchronized
    private fun disconnectWebSocket() {
        if (!isConnected) {
            // 当前WebSocket服务未连接，直接返回
            return
        }
        webSocket.close(STATE_CODE_NORMAL_CLOSURE, null)
        isConnected = false
    }

    /**
     * 当成功连接到WebSocket服务时回调此方法
     * @param webSocket WebSocket通道对象
     * @param response 服务响应数据
     */
    override fun onOpen(webSocket: WebSocket, response: Response) {
        isOpen = true
        // 启动自动登录、自动发送心跳包任务
        sessionJob = scope.launch {
            val loginCommandBody: JsonObject = JsonObject().apply {
                addProperty("appid", appWidgetConfiguration.appId)
                addProperty("uid", appWidgetConfiguration.userId)
                addProperty("token", appWidgetConfiguration.userToken)
                addProperty("p0_type", "attrs_v4")
                addProperty("heartbeat_interval", 150)
                addProperty("auto_subscribe", true)
            }
            // 循环请求登陆，直到成功登陆
            while(isActive) {
                // 检测WebSocket服务是否断开，断开这退出当前任务
                if (!isOpen) {
                    return@launch
                }
                // 发送登陆指令数据，等待响应
                if (sendCommandRequest(COMMAND_LOGIN_REQUEST, loginCommandBody)) {
                    val socketResponse: WebSocketResponse? = responseFlow.firstResponse(
                        command = COMMAND_LOGIN_RESPONSE,
                        timeMillis = 1500
                    )
                    if (socketResponse == null) {
                        // 等待响应超时，延时重试
                        delay(1500)
                        continue
                    }
                    val isSuccess: Boolean = socketResponse.data.get("success")?.asBoolean ?: false
                    if (!isSuccess) {
                        // 登陆失败，延时重试
                        delay(2000)
                        continue
                    }
                    // 登录成功，退出循环
                    break
                } else {
                    // 发送登陆指令失败，延时1.5秒重试
                    delay(1500)
                }
            }
            // 登陆成功，请求更新用户以及设备的信息
            updateUserDeviceList()
            updateControlDeviceListState()
            updateStateDeviceListState()
            // 循环发送心跳包，每隔15秒发送一次
            while (isActive && isOpen) {
                sendCommandRequest(COMMAND_HEART_BEAT)
                delay(15000)
            }
        }
    }

    /**
     * 当WebSocket服务响应数据时回调此方法
     * @param webSocket WebSocket通道对象
     * @param text 服务返回的消息
     */
    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            parser.fromJson(text, WebSocketResponse::class.java).apply {
                when (command) {
                    // 处理设备连接状态变更事件
                    COMMAND_CONNECTION_STATE -> onHandleConnectionStateResponse(this)
                    // 处理设备状态数据变更事件
                    COMMAND_DEVICE_DATA_STATE -> onHandleDeviceStateResponse(this)
                }
                responseFlow.tryEmit(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 处理设备的连接状态消息，分发设备的连接状态
     * @param response 设备连接状态变更消息
     */
    private fun onHandleConnectionStateResponse(response: WebSocketResponse) {
        // 获取基础信息
        val deviceId: String = response.data.get("did")?.asString ?: return
        val isOnline: Boolean = response.data.get("online")?.asBoolean ?: return
        onHandleConnectionStateResponse(deviceId, isOnline)
    }

    /**
     * 处理设备的连接状态消息，分发设备的连接状态
     * @param deviceId 设备ID
     * @param isOnline 设备在线状态
     */
    private fun onHandleConnectionStateResponse(deviceId: String, isOnline: Boolean) {
        scope.launch {
            // 分发事件到控制小组件
            run controlWidgetEventDispatch@ {
                controlConfigurationList.forEach {
                    if (it.deviceId == deviceId) {
                        controlConnectionChangedCallback(deviceId, isOnline)
                        return@controlWidgetEventDispatch
                    }
                }
            }
            // 分发事件到状态小组件
            run stateWidgetEventDispatch@ {
                stateConfigurationList.forEach {
                    if (it.deviceId == deviceId) {
                        stateConnectionChangedCallback(deviceId, isOnline)
                        return@stateWidgetEventDispatch
                    }
                }
            }
        }
    }

    /**
     * 处理设备的数据点状态变更消息，分发设备的状态数据
     * @param response 数据点状态变更消息
     */
    private fun onHandleDeviceStateResponse(response: WebSocketResponse) {
        // 获取基础信息
        val deviceId: String = response.data.get("did")?.asString ?: return
        val attributes: JsonObject = response.data.getAsJsonObject("attrs") ?: return
        // 分发设备状态事件
        scope.launch {
            // 分发事件到控制小组件
            run controlWidgetEventDispatch@ {
                controlConfigurationList.forEach {
                    if (it.deviceId == deviceId) {
                        controlDataStateChangedCallback(deviceId, attributes)
                        return@controlWidgetEventDispatch
                    }
                }
            }
            // 分发事件到状态小组件
            run stateWidgetEventDispatch@ {
                stateConfigurationList.forEach {
                    if (it.deviceId == deviceId) {
                        stateDataStateChangedCallback(deviceId, attributes)
                        return@stateWidgetEventDispatch
                    }
                }
            }
        }
    }

    /**
     * 当WebSocket服务发生异常时回调此方法
     * @param webSocket WebSocket通道对象
     * @param throwable 产生此错误的异常
     * @param
     */
    override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
        isOpen = false
        sessionJob?.cancel()
        // 尝试重连WebSocket服务
        scope.launch {
            delay(2000)
            reconnectWebSocket(webSocket)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        isOpen = false
        sessionJob?.cancel()
        // 如果关闭码不为STATE_CODE_NORMAL_CLOSURE，则尝试重连
        if (code != STATE_CODE_NORMAL_CLOSURE) {
            scope.launch {
                delay(2000)
                reconnectWebSocket(webSocket)
            }
        }
    }

    /**
     * 更新用户绑定的设备列表信息，如果更新请求失败则重试
     */
    private fun updateUserDeviceList() {
        scope.launch {
            repeat(5) {
                try {
                    openApi.fetchUserDeviceList().apply {
                        if (!isSuccess()) {
                            // 请求用户绑定设备失败，延时重试
                            delay(1500)
                            return@repeat
                        }
                        devices.forEach {
                            onHandleConnectionStateResponse(it.deviceId, it.isOnline)
                            controlDeviceInformationChangedCallback(it.deviceId, it.deviceName)
                        }
                        // 请求用户绑定设备成功
                        return@launch
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 更新控制小组件的设备列表中的设备状态
     */
    private fun updateControlDeviceListState() {
        controlConfigurationList.forEach { controlConfiguration ->
            scope.launch {
                repeat(5) {
                    val readDeviceStateBody: JsonObject = JsonObject().apply {
                        addProperty("did", controlConfiguration.deviceId)
                    }
                    // 发送设备状态请求指令
                    sendCommandRequest(COMMAND_READ_DEVICE_DATA_REQUEST, readDeviceStateBody)
                    // 等待接收响应
                    val response: WebSocketResponse? = responseFlow.firstResponse(
                        command = COMMAND_READ_DEVICE_DATA_RESPONSE,
                        timeMillis = 1500
                    )
                    val isSuccess: Boolean = response?.data?.get("success")?.asBoolean ?: false
                    if (response == null || !isSuccess) {
                        // 响应失败，延时重试
                        delay(1500)
                        return@repeat
                    }
                    // 获取设备状态成功
                    return@launch
                }
            }
        }
    }

    /**
     * 更新状态小组件的设备列表中的设备状态
     */
    private fun updateStateDeviceListState() {
        stateConfigurationList.forEach { stateConfiguration ->
            scope.launch {
                repeat(5) {
                    val readDeviceStateBody: JsonObject = JsonObject().apply {
                        addProperty("did", stateConfiguration.deviceId)
                    }
                    // 发送设备状态请求指令
                    sendCommandRequest(COMMAND_READ_DEVICE_DATA_REQUEST, readDeviceStateBody)
                    // 等待接收响应
                    val response: WebSocketResponse? = responseFlow.firstResponse(
                        command = COMMAND_READ_DEVICE_DATA_RESPONSE,
                        timeMillis = 1500
                    )
                    val isSuccess: Boolean = response?.data?.get("success")?.asBoolean ?: false
                    if (response == null || !isSuccess) {
                        // 响应失败，延时重试
                        delay(1500)
                        return@repeat
                    }
                    // 获取设备状态成功
                    return@launch
                }
            }
        }
    }

    /**
     * 向WebSocket服务发送指令请求数据
     * @param command 指令命令
     * @param data 指令数据
     * @return 请求响应
     */
    private fun sendCommandRequest(command: String, data: JsonObject? = null): Boolean {
        if (!isConnected || !isOpen) {
            return false
        }
        val commandBody: JsonObject = JsonObject()
        commandBody.addProperty("cmd", command)
        if (data != null) {
            commandBody.add("data", data)
        }
        return webSocket.send(commandBody.toString())
    }

    /**
     * 从WebSocket服务响应数据流中获取首个指令配对成功的响应
     * @param command 响应的指令值
     * @param timeMillis 超时时间，单位：毫秒
     * @return 服务响应消息，如果超时则为null
     */
    private suspend fun Flow<WebSocketResponse>.firstResponse(
        command: String,
        timeMillis: Long
    ): WebSocketResponse? {
        return withTimeoutOrNull(timeMillis) {
            first {
                it.command == command
            }
        }
    }

}

private class WebSocketResponse {

    @SerializedName("cmd")
    val command: String = ""

    @SerializedName("data")
    val data: JsonObject = JsonObject()

}











