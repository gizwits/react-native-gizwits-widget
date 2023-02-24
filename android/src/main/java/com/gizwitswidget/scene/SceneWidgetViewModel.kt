package com.gizwitswidget.scene

import android.app.Application
import android.graphics.Color
import android.util.Log
import com.gizwitswidget.AppWidgetWebSocketService
import com.gizwitswidget.R
import com.gizwitswidget.WidgetViewModel
import com.gizwitswidget.core.data.repository.WidgetApiRepository
import com.gizwitswidget.core.data.repository.WidgetApiRepositoryImpl
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepository
import com.gizwitswidget.core.data.repository.WidgetConfigurationRepositoryImpl
import com.gizwitswidget.model.AppWidgetConfiguration
import com.gizwitswidget.model.ControlConfiguration
import com.gizwitswidget.model.SceneConfiguration
import com.gizwitswidget.network.WidgetApiResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SceneWidgetViewModel(application: Application) : WidgetViewModel(application) {

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
    private val isSceneWidgetEnabled: Flow<Boolean> = widgetConfigurationRepository
        .getSceneWidgetEnabled(application)

    /**
     * 场景小组件的配置文件
     */
    private val sceneConfigurations: Flow<List<SceneConfiguration>> =
        widgetConfigurationRepository
            .getSceneWidgetConfiguration(application)
            .map { configuration ->
                if (configuration != null) {
                    val collectionType: TypeToken<List<SceneConfiguration>> =
                        object : TypeToken<List<SceneConfiguration>>() {}
                    configurationParser.fromJson(configuration, collectionType.type)
                } else {
                    listOf()
                }
            }

    /**
     * 被执行的场景状态信息列表
     */
    private val executedSceneInfoList: MutableList<ExecutedSceneInfo> = mutableListOf()

    /**
     * 被执行场景的家id以及场景以及场景任务对象的映射集合
     */
    private val executedSceneJobMap: MutableMap<String, Job> = mutableMapOf()

    /**
     * 被执行场景的状态信息
     */
    private val executedSceneInfos: MutableSharedFlow<List<ExecutedSceneInfo>> =
        MutableSharedFlow<List<ExecutedSceneInfo>>(replay = 1).apply { tryEmit(listOf()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _sceneWidgetUiState: StateFlow<SceneWidgetUiState> =
        isSceneWidgetEnabled.transformLatest { isEnabled ->
            if (isEnabled) {
                // 当前桌面存在小组件，开启小组件服务
                val sceneWidgetUiState: Flow<SceneWidgetUiState> =
                    combineLatest(
                        sceneConfigurations,
                        executedSceneInfos
                    ) { _sceneConfigurations, _executedSceneInfos ->
                        SceneWidgetUiState.Success(
                            itemStateList = _sceneConfigurations.map { sceneConfiguration ->
                                val executedSceneInfo: ExecutedSceneInfo? = _executedSceneInfos
                                    .find { it.homeId == sceneConfiguration.homeId && it.sceneId == sceneConfiguration.id }
                                val sceneTitle: String = executedSceneInfo?.sceneTitle ?: sceneConfiguration.name
                                val iconTint: Int = executedSceneInfo?.iconTint ?: Color.WHITE
                                val backgroundResId: Int = executedSceneInfo?.backgroundResId ?: R.drawable.scene_widget_item_background
                                SceneWidgetItemState(
                                    homeId = sceneConfiguration.homeId,
                                    sceneId = sceneConfiguration.id,
                                    sceneTitle = sceneTitle,
                                    iconName = sceneConfiguration.iconName,
                                    iconTint = iconTint,
                                    backgroundResId = backgroundResId
                                )
                            }
                        )
                    }
                emitAll(sceneWidgetUiState)
            } else {
                // 当前桌面无小组件，关闭小组件服务
                emit(SceneWidgetUiState.Idle)
            }
        }.onEach{
            SceneWidgetProvider.updateAppWidget(application)
            SceneWidgetProvider.notifyAppWidgetViewDataChanged(application)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SceneWidgetUiState.Idle
        )

    val sceneWidgetUiState: SceneWidgetUiState
        get() = _sceneWidgetUiState.value

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            widgetConfigurationRepository.setSceneWidgetEnabled(
                context = application,
                enabled = enabled
            )
        }
    }

    /**
     * 执行指定场景
     * @param homeId 家id
     * @param sceneId 场景id
     */
    fun executeScene(homeId: Int, sceneId: Int) {
        val executedSceneJobKey: String = "$homeId + $sceneId"
        val lastExecutedSceneJob: Job? = executedSceneJobMap[executedSceneJobKey]
        val newExecutedSceneJob: Job = viewModelScope.launch {
            // 取消执行上一次任务
            lastExecutedSceneJob?.cancelAndJoin()
            // 开始执行本次任务
            val executedSceneInfo: ExecutedSceneInfo = ExecutedSceneInfo(homeId, sceneId)
            executedSceneInfoList.add(executedSceneInfo)
            try {
                // 获取通用的小组件配置
                val commonConfigurationStr: String = widgetConfigurationRepository
                    .getCommonWidgetConfiguration(application).first() ?: return@launch
                val commonConfiguration: AppWidgetConfiguration = configurationParser
                    .fromJson(commonConfigurationStr, AppWidgetConfiguration::class.java)
                val widgetApiRepository: WidgetApiRepository = WidgetApiRepositoryImpl(
                    openUrl = commonConfiguration.openUrl,
                    aepUrl = commonConfiguration.aepUrl,
                    appId = commonConfiguration.appId,
                    userToken = commonConfiguration.userToken
                )
                // 显示执行中状态
                executedSceneInfo.apply {
                    sceneTitle = application.resources.getString(R.string.hint_in_execution)
                    iconTint = Color.parseColor(commonConfiguration.tintColor)
                    backgroundResId =  R.drawable.scene_widget_item_active_background
                }
                executedSceneInfos.emit(executedSceneInfoList.toList())
                // 请求执行场景
                val responseResult: Result<WidgetApiResponse> = widgetApiRepository.executeUserScene(homeId, sceneId)
                if (responseResult.isFailure || !responseResult.getOrThrow().isSuccess()) {
                    // 显示执行失败状态
                    executedSceneInfo.apply {
                        sceneTitle = application.resources.getString(R.string.hint_execution_failure)
                        iconTint = Color.parseColor(commonConfiguration.tintColor)
                        backgroundResId = R.drawable.scene_widget_item_active_background
                    }
                } else {
                    // 显示执行成功状态
                    executedSceneInfo.apply {
                        sceneTitle = application.resources.getString(R.string.hint_execution_success)
                        iconTint = Color.parseColor(commonConfiguration.tintColor)
                        backgroundResId = R.drawable.scene_widget_item_active_background
                    }
                }
                executedSceneInfos.emit(executedSceneInfoList.toList())
                delay(2000)
            } finally {
                withContext(NonCancellable) {
                    // 任务执行完成移除所有任务效果
                    executedSceneInfoList.remove(executedSceneInfo)
                    executedSceneInfos.emit(executedSceneInfoList.toList())
                }
            }
        }
        executedSceneJobMap[executedSceneJobKey] = newExecutedSceneJob
    }

}

sealed interface SceneWidgetUiState {

    /* 空闲状态，未加载任何小组件数据 */
    object Idle : SceneWidgetUiState

    /* 已加载状态，显示场景小组件列表 */
    data class Success(
        val itemStateList: List<SceneWidgetItemState>
    ) : SceneWidgetUiState

}

data class SceneWidgetItemState(
    val homeId: Int,
    val sceneId: Int,
    val sceneTitle: String,
    val iconName: String,
    val iconTint: Int,
    val backgroundResId: Int
)

data class ExecutedSceneInfo(
    val homeId: Int,
    val sceneId: Int,
    var sceneTitle: String? = null,
    var iconTint: Int? = null,
    var backgroundResId: Int? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun <T1, T2, R> combineLatest(
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