package com.gizwitswidget.network

import com.gizwitswidget.network.UserDeviceList
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenApi {

    /**
     * 请求获取关联到用户的设备列表
     * @param limit 设备列表个数限制
     */
    @GET("app/bindings")
    suspend fun fetchUserDeviceList(@Query("limit") limit: Int): UserDeviceList

}
