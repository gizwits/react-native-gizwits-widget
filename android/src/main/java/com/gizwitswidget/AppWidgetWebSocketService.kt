package com.gizwitswidget

import android.content.Context
import com.gizwitswidget.core.data.repository.WidgetApiRepository
import com.gizwitswidget.core.data.repository.WidgetApiRepositoryImpl
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepository
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepositoryImpl
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.network.UserDevice
import com.gizwitswidget.network.UserDeviceList
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*

object AppWidgetWebSocketService {

    /**
     * 用于解析传入的小组件相关配置信息的解析器
     */
    private val configurationParser: Gson = GsonBuilder().create()

    /**
     * 小组件的WebSocket服务的携程作用域
     */
    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob())

    /**
     * 小组件配置文件存储库，用于更新获取对应应用小组件的配置文件
     */
    private val widgetConfigurationRepository: WidgetConfigurationRepository =
        WidgetConfigurationRepositoryImpl.getInstance()

    /**
     * 连接WebSocket服务的业务锁
     */
    private val connectMutex: Mutex = Mutex()

    /**
     * 当前连接着WebSocket服务的客户端个数
     */
    @Volatile
    private var connectionCount: Int = 0

    /**
     * 标志位，标记当前服务是否已成功开始连接服务（调用[connect]）
     */
    @Volatile
    private var isServiceConnected: Boolean = false

    /**
     * 未发布的WebSocket服务实现
     */
    private val unpublishedWebSocketService: WebSocketService = WebSocketService(true)

    /**
     * 已发布的WebSocket服务实现
     */
    private val publishedWebSocketService: WebSocketService = WebSocketService(false)

    /**
     * 设备连接状态监听者集合
     */
    private val connectionStateListenerSet: MutableSet<(String, Boolean) -> Unit> = mutableSetOf()

    /**
     * 设备的状态监听者集合
     */
    private val deviceStateListenerSet: MutableSet<(String, JsonObject) -> Unit> = mutableSetOf()

    /**
     * 连接小组件的WebSocket服务
     * @param context 上下文对象
     * @param commonConfiguration 应用小组件全局通用配置
     */
    suspend fun connect(
        context: Context,
        commonConfiguration: AppWidgetConfiguration
    ) = connectMutex.withLock {
        // 客户端的连接数加1
        connectionCount += 1
        if (isServiceConnected) {
            // 如果服务已开始连接，则忽略
            return@withLock
        }
        // 开始连接小组件的WebSocket服务
        publishedWebSocketService.connect(commonConfiguration)
        unpublishedWebSocketService.connect(commonConfiguration)
        isServiceConnected = true
    }

    /**
     * 断开与小组件的WebSocket服务
     */
    suspend fun disconnect() = connectMutex.withLock {
        if (connectionCount > 0) {
            connectionCount -= 1
        } else {
            return@withLock
        }
        if (connectionCount == 0) {
            // 断开小组件的WebSocket服务
            publishedWebSocketService.disconnect()
            unpublishedWebSocketService.disconnect()
            isServiceConnected = false
        }
    }

    fun subscribeConnectionState(listener: (String, Boolean) -> Unit) {
        connectionStateListenerSet.add(listener)
    }

    fun unsubscribeConnectionState(listener: (String, Boolean) -> Unit) {
        connectionStateListenerSet.remove(listener)
    }

    /**
     * 处理分发设备的连接状态变更事件消息
     * @param response 响应的消息
     */
    fun onHandleConnectionStateResponse(response: WebSocketResponse) {
        val deviceId: String = response.data.get("did")?.asString ?: return
        val isOnline: Boolean = response.data.get("online")?.asBoolean ?: return
        onHandleConnectionStateResponse(deviceId, isOnline)
    }

    /**
     * 处理分发设备的连接状态变更事件消息
     * @param deviceId 设备id
     * @param isOnline 设备在线状态
     */
    fun onHandleConnectionStateResponse(deviceId: String, isOnline: Boolean) {
        serviceScope.launch {
            // 分发事件
            connectionStateListenerSet.forEach {
                it(deviceId, isOnline)
            }
        }
    }

    fun subscribeDeviceState(listener: (String, JsonObject) -> Unit) {
        deviceStateListenerSet.add(listener)
    }

    fun unsubscribeDeviceState(listener: (String, JsonObject) -> Unit) {
        deviceStateListenerSet.remove(listener)
    }

    /**
     * 处理分发设备的状态变更事件消息
     * @param response 响应的消息
     */
    fun onHandleDeviceStateResponse(response: WebSocketResponse) {
        serviceScope.launch {
            val deviceId: String = response.data.get("did")?.asString ?: return@launch
            val attributes: JsonObject = response.data.getAsJsonObject("attrs") ?: return@launch
            // 分发事件
            deviceStateListenerSet.forEach {
                it(deviceId, attributes)
            }
        }
    }

    /**
     * 执行设备数据点控制
     * @param deviceId 设备ID
     * @param attributes 写入控制的属性字段信息
     */
    fun executeControl(deviceId: String, attributes: JsonObject, isSandBox: Boolean) {
        if (isSandBox) {
            unpublishedWebSocketService.executeControl(deviceId, attributes)
        } else {
            publishedWebSocketService.executeControl(deviceId, attributes)
        }
    }

    class WebSocketService(val isSandBox: Boolean) : WebSocketListener() {

        /**
         * 应用小组件的通用配置信息
         */
        private lateinit var appWidgetConfiguration: AppWidgetConfiguration

        /**
         * 小组件的Api接口服务存储库
         */
        private lateinit var widgetApiRepository: WidgetApiRepository

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
         * 服务回调的消息数据流
         */
        private val responseFlow: MutableSharedFlow<WebSocketResponse> =
            MutableSharedFlow(extraBufferCapacity = 1)

        /**
         * 标志位，指示当前是否已经成功连接到WebSocket服务（[onOpen]被调用）
         */
        private var isOpen: Boolean = false

        /**
         * 通过指定的应用配置信息，连接至指定的WebSocket服务
         * @param configuration 通用的小组件配置信息
         */
        @Synchronized
        fun connect(configuration: AppWidgetConfiguration) {
            appWidgetConfiguration = configuration
            val request: Request = Request.Builder()
                .url(
                    if (isSandBox) {
                        appWidgetConfiguration.m2mStageUrl
                    } else {
                        appWidgetConfiguration.m2mUrl
                    }
                ).build()
            widgetApiRepository = WidgetApiRepositoryImpl(
                openUrl = configuration.openUrl,
                aepUrl = configuration.aepUrl,
                appId = configuration.appId,
                userToken = configuration.userToken
            )
            webSocket = webSocketClient.newWebSocket(request, this)
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
            webSocket.close(STATE_CODE_NORMAL_CLOSURE, null)
        }

        /**
         * 当成功连接到WebSocket服务时回调此方法
         * @param webSocket WebSocket通道对象
         * @param response 服务响应数据
         */
        override fun onOpen(webSocket: WebSocket, response: Response) {
            isOpen = true
            // 启动自动登录，自动发送心跳包任务
            sessionJob = serviceScope.launch {
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
                // 刷新设备的在线状态以及数据点信息
                updateDeviceInformation()
                // 循环发送心跳包，每隔15秒发送一次
                while (isActive && isOpen) {
                    sendCommandRequest(COMMAND_HEART_BEAT)
                    delay(15000)
                }
            }
        }

        /**
         * 更新设备信息任务
         */
        private var updateDeviceInformationJob: Job? = null

        /**
         * 在WebSocket服务之后调用，获取设备的最新连接状态以及数据点信息
         */
        private fun updateDeviceInformation() {
            val lastUpdateDeviceInformationJob: Job? = updateDeviceInformationJob
            updateDeviceInformationJob = serviceScope.launch {
                // 先取消先前的任务
                lastUpdateDeviceInformationJob?.cancelAndJoin()
                // 获取用户的绑定信息
                var userDeviceList: List<UserDevice>? = null
                while (isActive) {
                    val UserDeviceListResult: Result<UserDeviceList> =
                        widgetApiRepository.fetchUserDeviceList()
                    if (UserDeviceListResult.isFailure || !UserDeviceListResult.getOrThrow().isSuccess()) {
                        // 请求设备列表信息失败，延时重试
                        delay(2000)
                        continue
                    } else {
                        userDeviceList = UserDeviceListResult.getOrThrow().devices
                        break
                    }
                }
                if (userDeviceList == null) {
                    return@launch
                }
                // 获取所有在线设备的状态信息
                userDeviceList.onEach {
                    // 发布设备在线状态信息
                    onHandleConnectionStateResponse(it.deviceId, it.isOnline)
                }.filter {
                    // 过滤出发布状态一致的设备
                    it.isSandbox == isSandBox
                }.filter {
                    // 过滤出在线的设备
                    it.isOnline
                }.forEach { userDevice ->
                    // 启动子携程循环获取设备状态直至成功
                    launch {
                        while (isActive) {
                            val readDeviceStateBody: JsonObject = JsonObject().apply {
                                addProperty("did", userDevice.deviceId)
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
                    // 延时才开始进行下一次设备状态获取
                    delay(500)
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
            serviceScope.launch {
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
                serviceScope.launch {
                    delay(2000)
                    reconnect(webSocket)
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
            if (!isOpen) {
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

        companion object {

            /**
             * WebSocket服务响应数据解析器
             */
            private val parser: Gson = GsonBuilder().create()

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

        }

    }

}

class WebSocketResponse {

    @SerializedName("cmd")
    val command: String = ""

    @SerializedName("data")
    val data: JsonObject = JsonObject()

}















