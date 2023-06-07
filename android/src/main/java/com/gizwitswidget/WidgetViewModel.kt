package com.gizwitswidget

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

abstract class WidgetViewModel(
    protected val application: Application
) {

    /**
     * 小组件的视图模型的协程作用域，作用域的范围为整个应用的生命周期
     */
    protected val viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob())

}