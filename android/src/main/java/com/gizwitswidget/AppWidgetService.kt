package com.gizwitswidget

import android.util.Log
import com.gizwitswidget.model.StateConfiguration
import com.gizwitswidget.network.OpenApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.model.ControlConfiguration
import com.gizwitswidget.network.HeaderManageInterceptor
import com.gizwitswidget.network.UserDeviceList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppWidgetService {

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
     * OpenApi接口对象
     */
    private lateinit var openApi: OpenApi

    /**
     * 应用小组件配置信息，为空则表示从未进行过注册初始化[registerWidgetService]，
     * 通过[connectWebSocket]接口初始化
     */
    private lateinit var appWidgetConfiguration: AppWidgetConfiguration

    /**
     * 控制设备状态列表
     */
    private var controlDeviceStateList: List<ControlDeviceState> = emptyList()

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
     * 状态设备状态列表
     */
    private var stateDeviceStateList: List<StateDeviceState> = emptyList()

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
     * 标志位，指示当前服务是否以及完成
     */
    private var isRegistered: Boolean = false

    /**
     * 未发布的WebSocket服务实现
     */
    private val unpublishedWebSocketService: WidgetWebSocketService =
        WidgetWebSocketService(true)

    /**
     * 已发布的WebSocket服务实现
     */
    private val publishedWebSocketService: WidgetWebSocketService =
        WidgetWebSocketService(false)

    /**
     * 更新用户设备列表任务，关联当前正在获取用户绑定设备列表的任务
     */
    private var updateUserDeviceListJob: Job? = null

    /**
     * 注册初始化应用小组件服务，此接口会被多次调用，用于更新全局通用配置
     * @param configuration 应用小组件配置
     */
    @Synchronized
    fun registerWidgetService(
        configuration: AppWidgetConfiguration
    ) {
        appWidgetConfiguration = configuration
        // 使用新的应用小组件配置信息连接WebSocket服务
        unpublishedWebSocketService.connect(configuration)
        publishedWebSocketService.connect(configuration)
        // 初始化、更新依赖对象
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
        controlDeviceStateList = controlConfigurations.map {
            ControlDeviceState(it)
        }
        controlConnectionChangedCallback = onDeviceConnectionChanged
        controlDeviceInformationChangedCallback = onDeviceInformationChanged
        controlDataStateChangedCallback = onDeviceStateChanged
        // 更新用户绑定设备列表，以及设备列表状态
        updateUserDeviceList()
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
        stateDeviceStateList = stateConfigurations.map {
            StateDeviceState(it)
        }
        stateConnectionChangedCallback = onDeviceConnectionChanged
        stateDataStateChangedCallback = onDeviceStateChanged
        // 更新用户绑定设备列表，以及设备列表状态
        updateUserDeviceList()
    }

    /**
     * 更新用户绑定的设备列表信息，如果更新请求失败则重试
     * @param onSuccess 当获取成功时回调的方法
     */
    private fun updateUserDeviceList(onSuccess: () -> Unit = {}) {
        val lastUpdateUserDeviceListJob: Job? = updateUserDeviceListJob
        updateUserDeviceListJob = scope.launch {
            // 如果存在，则取消先前的请求任务并等待其完成
            lastUpdateUserDeviceListJob?.cancelAndJoin()
            // 循环获取用户绑定设备列表直到成功
            while (isActive) {
                try {
                    val userDeviceList: UserDeviceList = openApi.fetchUserDeviceList()
                    if (userDeviceList.isSuccess()) {
                        // 分发设备的状态信息
                        userDeviceList.devices.forEach {
                            // 分发设备的连接状态信息
                            onHandleConnectionStateResponse(it.deviceId, it.isOnline)
                            // 分发设备的发布状态信息
                            onHandlePublishState(it.deviceId, !it.isSandbox)
                            // 通知设备信息已发生改变
                            controlDeviceInformationChangedCallback(it.deviceId, it.deviceName)
                        }
                        // 更新用户信息成功，刷新小组件的设备列表状态
                        launch {
                            updateControlDeviceListState()
                        }
                        launch {
                            updateStateDeviceListState()
                        }
                        return@launch
                    } else {
                        // 请求失败，延时1.5秒之后重试
                        delay(1500)
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
    private suspend fun updateControlDeviceListState() {
        unpublishedWebSocketService.updateControlDeviceListState()
        publishedWebSocketService.updateControlDeviceListState()
    }

    /**
     * 更新状态小组件的设备列表中的设备状态
     */
    private suspend fun updateStateDeviceListState() {
        unpublishedWebSocketService.updateStateDeviceListState()
        publishedWebSocketService.updateStateDeviceListState()
    }

    /**
     * 执行设备数据点控制
     * @param deviceId 设备ID
     * @param attributes 写入控制的属性字段信息
     */
    fun executeControl(deviceId: String, attributes: JsonObject) {
        controlDeviceStateList.forEach {
            if (it.deviceId == deviceId) {
                when (it.isSandBox) {
                    false -> publishedWebSocketService.executeControl(deviceId, attributes)
                    true -> unpublishedWebSocketService.executeControl(deviceId, attributes)
                    else -> Unit
                }
                return@executeControl
            }
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
     * 处理设备的连接状态消息，分发设备的连接状态，通过OpenApi或者WebSocket更新获取
     * @param deviceId 设备ID
     * @param isOnline 设备在线状态
     */
    private fun onHandleConnectionStateResponse(deviceId: String, isOnline: Boolean) {
        scope.launch {
            // 分发事件到控制小组件
            run controlWidgetEventDispatch@ {
                controlDeviceStateList.forEach {
                    if (it.deviceId == deviceId) {
                        controlConnectionChangedCallback(deviceId, isOnline)
                        return@controlWidgetEventDispatch
                    }
                }
            }
            // 分发事件到状态小组件
            run stateWidgetEventDispatch@ {
                stateDeviceStateList.forEach {
                    if (it.deviceId == deviceId) {
                        stateConnectionChangedCallback(deviceId, isOnline)
                        return@stateWidgetEventDispatch
                    }
                }
            }
        }
    }

    /**
     * 处理设备的发布状态，分发更新设备的发布状态，通过OpenApi获取
     * @param deviceId 设备ID
     * @param isPublished 设备的发布状态
     */
    private fun onHandlePublishState(deviceId: String, isPublished: Boolean) {
        scope.launch {
            // 分发事件到控制小组件
            run controlWidgetEventDispatch@ {
                controlDeviceStateList.forEach {
                    if (it.deviceId == deviceId) {
                        it.isSandBox = !isPublished
                        return@controlWidgetEventDispatch
                    }
                }
            }
            // 分发事件到状态小组件
            run stateWidgetEventDispatch@ {
                stateDeviceStateList.forEach {
                    if (it.deviceId == deviceId) {
                        it.isSandBox = !isPublished
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
                controlDeviceStateList.forEach {
                    if (it.deviceId == deviceId) {
                        controlDataStateChangedCallback(deviceId, attributes)
                        return@controlWidgetEventDispatch
                    }
                }
            }
            // 分发事件到状态小组件
            run stateWidgetEventDispatch@ {
                stateDeviceStateList.forEach {
                    if (it.deviceId == deviceId) {
                        stateDataStateChangedCallback(deviceId, attributes)
                        return@stateWidgetEventDispatch
                    }
                }
            }
        }
    }

    class WidgetWebSocketService(val isSandBox: Boolean) : WebSocketListener() {

        /**
         * 标志位，指示当前是否处于连接WebSocket服务的状态（逻辑上），当处于[connectWebSocket]
         * 和[disconnectWebSocket]调用之间时，此值为true，与实际的连接状态无关
         */
        private var isConnected: Boolean = false

        /**
         * 应用小组件配置信息，为空则表示从未进行过注册初始化[registerWidgetService]，
         * 通过[connectWebSocket]接口初始化
         */
        private lateinit var appWidgetConfiguration: AppWidgetConfiguration

        /**
         * WebSocket客户端
         */
        private val webSocketClient: OkHttpClient = OkHttpClient.Builder().build()

        /**
         * WebSocket连接对象
         */
        private lateinit var webSocket: WebSocket

        /**
         * WebSocket服务相关会话业务管理任务
         */
        private var sessionJob: Job? = null

        /**
         * 标志位，指示当前是否已经成功连接到WebSocket服务（[onOpen]被调用）
         */
        private var isOpen: Boolean = false

        /**
         * 服务回调的消息数据流
         */
        private val responseFlow: MutableSharedFlow<WebSocketResponse> =
            MutableSharedFlow(extraBufferCapacity = 1)

        /**
         * 使用指定的小组件配置信息连接至WebSocket服务，如果当前服务已连接且配置信息
         * 相等，则忽略此请求，否则创建新连接
         * @param configuration 应用小组件配置信息
         */
        @Synchronized
        fun connect(configuration: AppWidgetConfiguration) {
            // 根据配置信息判断当前服务的状态是否需要重连，需要则进行重连
            if (isConnected) {
                // 处于连接状态，则小组件配置信息肯定不为空
                val oldConfiguration: AppWidgetConfiguration = appWidgetConfiguration
                if (oldConfiguration.appId != configuration.appId ||
                    oldConfiguration.userId != configuration.userId ||
                    oldConfiguration.userToken != configuration.userToken ||
                    oldConfiguration.m2mUrl != configuration.m2mUrl) {
                    // 配置发生改变，断开连接进行重连
                    disconnect()
                } else {
                    // 配置未发生改变，无动作
                    return
                }
            }
            // 当服务未连接或者服务连接配置信息更新时，则进行连接
            appWidgetConfiguration = configuration
            val request: Request = Request.Builder()
                .url(
                    if (isSandBox) {
                        appWidgetConfiguration.m2mStageUrl
                    } else {
                        appWidgetConfiguration.m2mUrl
                    }
                ).build()
            webSocket = webSocketClient.newWebSocket(request, this)
            isConnected = true
        }

        @Synchronized
        private fun reconnect(oldWebSocket: WebSocket) {
            if (webSocket != oldWebSocket) {
                return
            }
            webSocket.close(STATE_CODE_NORMAL_CLOSURE, null)
            val request: Request = Request.Builder()
                .url(
                    if (isSandBox) {
                        appWidgetConfiguration.m2mStageUrl
                    } else {
                        appWidgetConfiguration.m2mUrl
                    }
                ).build()
            webSocket = webSocketClient.newWebSocket(request, this)
        }

        @Synchronized
        fun disconnect() {
            if (!isConnected) {
                // 当前WebSocket服务未连接，直接返回
                return
            }
            webSocket.close(STATE_CODE_NORMAL_CLOSURE, null)
            isConnected = false
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
         * 当成功连接到WebSocket服务时回调此方法
         * @param webSocket WebSocket通道对象
         * @param response 服务响应数据
         */
        override fun onOpen(webSocket: WebSocket, response: Response) {
            isOpen = true
            // 启动自动登录，自动发送心跳包任务
            sessionJob = scope.launch {
                val loginCommandBody: JsonObject = JsonObject().apply {
                    addProperty("appid", appWidgetConfiguration.appId)
                    addProperty("uid", appWidgetConfiguration.userId)
                    addProperty("token", appWidgetConfiguration.userToken)
                    addProperty("p0_type", "attrs_v4")
                    addProperty("heartbeat_interval", 150)
                    addProperty("auto_subscribe", true)
                }
                // 循环请求登录，直到成功登录
                while (isActive) {
                    // 检测WebSocket服务是否断开，断开则退出当前任务
                    if (!isOpen) {
                        return@launch
                    }
                    // 发送登录指令数据，等待响应
                    if (sendCommandRequest(COMMAND_LOGIN_REQUEST, loginCommandBody)) {
                        val socketResponse: WebSocketResponse? = responseFlow.firstResponse(
                            command = COMMAND_LOGIN_RESPONSE,
                            timeMillis = 1500
                        )
                        val isSuccess: Boolean = socketResponse?.data?.get("success")?.asBoolean ?: false
                        if (!isSuccess) {
                            // 登陆失败，延时重试
                            delay(2000)
                            continue
                        }
                        // 登录成功，退出循环
                        break
                    } else {
                        // 发送登录指令失败，延时1.5秒重试
                        delay(1500)
                    }
                }
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
         * 当WebSocket服务发生异常时回调此方法
         * @param webSocket WebSocket通道对象
         * @param throwable 产生此错误的异常
         * @param response
         */
        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
            isOpen = false
            sessionJob?.cancel()
            // 尝试重连WebSocket服务
            scope.launch {
                delay(2000)
                reconnect(webSocket)
            }
        }

        /**
         * 当WebSocket服务关闭时，检测服务的关闭状态是否正常，不正常则重新连接服务
         */
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            isOpen = false
            sessionJob?.cancel()
            // 如果关闭码不为STATE_CODE_NORMAL_CLOSURE，则尝试重连
            if (code != STATE_CODE_NORMAL_CLOSURE) {
                scope.launch {
                    delay(2000)
                    reconnect(webSocket)
                }
            }
        }

        /**
         * 更新控制小组件的设备列表中[isSandBox]字段一致的设备的状态
         */
        suspend fun updateControlDeviceListState() {
            coroutineScope {
                // 遍历所有待更新状态设备的设备列表
                controlDeviceStateList.filter {
                    it.isSandBox == isSandBox
                }.forEach { controlDeviceState ->
                    launch {
                        while (isActive) {
                            val readDeviceStateBody: JsonObject = JsonObject().apply {
                                addProperty("did", controlDeviceState.deviceId)
                            }
                            // 发送设备状态请求指令
                            sendCommandRequest(COMMAND_READ_DEVICE_DATA_REQUEST, readDeviceStateBody)
                            // 等待接收响应
                            val response: WebSocketResponse? = responseFlow.firstResponse(
                                command = COMMAND_READ_DEVICE_DATA_RESPONSE,
                                timeMillis = 1500
                            )
                            val isSuccess: Boolean = response != null
                            if (isSuccess) {
                                // 获取成功
                                break
                            } else {
                                // 获取失败，延时重试
                                delay(2000)
                            }
                        }
                    }
                    // 延时才开始进行下一个设备状态更新
                    delay(500)
                }
            }
        }

        /**
         * 更新状态小组件的设备列表中[isSandBox]字段一致的设备的状态
         */
        suspend fun updateStateDeviceListState() {
            coroutineScope {
                // 遍历待更新状态设备的设备列表
                stateDeviceStateList.filter {
                    it.isSandBox == isSandBox
                }.forEach { stateDeviceState ->
                    launch {
                        while (isActive) {
                            val readDeviceStateBody: JsonObject = JsonObject().apply {
                                addProperty("did", stateDeviceState.deviceId)
                            }
                            // 发送设备状态请求指令
                            sendCommandRequest(COMMAND_READ_DEVICE_DATA_REQUEST, readDeviceStateBody)
                            // 等待接收响应
                            val response: WebSocketResponse? = responseFlow.firstResponse(
                                command = COMMAND_READ_DEVICE_DATA_RESPONSE,
                                timeMillis = 1500
                            )
                            val isSuccess: Boolean = response != null
                            if (isSuccess) {
                                // 获取成功
                                break
                            } else {
                                // 获取失败，延时重试
                                delay(1500)
                            }
                        }
                    }
                    // 延时才开始进行下一个设备状态更新
                    delay(500)
                }
            }
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

}

private class ControlDeviceState(configuration: ControlConfiguration) {

    val deviceId: String = configuration.deviceId

    var isSandBox: Boolean? = null

}

private class StateDeviceState(configuration: StateConfiguration) {

    val deviceId: String = configuration.deviceId

    var isSandBox: Boolean? = null

}

private class WebSocketResponse {

    @SerializedName("cmd")
    val command: String = ""

    @SerializedName("data")
    val data: JsonObject = JsonObject()

}











