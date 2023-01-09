package com.gizwitswidget.network

import com.gizwits.xb.network.UserDeviceList
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenApi {

    /**
     * 请求获取关联到用户的设备列表
     */
    @GET("app/bindings")
    suspend fun fetchUserDeviceList(): UserDeviceList

}
