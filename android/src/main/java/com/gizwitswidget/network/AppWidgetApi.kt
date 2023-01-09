package com.gizwits.xb.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AppWidgetApi {

    /**
     * 请求获取关联到用户的场景列表
     * @return 关联到用户的场景列表
     */
    @GET("app/smartHome/homes/0/manual_scenes")
    suspend fun fetchUserSceneList(): UserSceneList

    /**
     * 请求执行场景
     * @param homeId 家庭ID
     * @param sceneId 场景ID
     */
    @POST("app/smartHome/homes/{homeId}/manual_scenes/{sceneId}/execute")
    suspend fun executeUserScene(
        @Path("homeId") homeId: Int,
        @Path("sceneId") sceneId: Int
    ): WidgetApiResponse

}
