package com.gizwitswidget.core.data.repository

import com.gizwitswidget.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Path

interface WidgetApiRepository {

    suspend fun fetchUserDeviceList(): Result<UserDeviceList>

    suspend fun executeUserScene(homeId: Int, sceneId: Int): Result<WidgetApiResponse>

}

class WidgetApiRepositoryImpl constructor(
    openUrl: String,
    aepUrl: String,
    appId: String,
    userToken: String
) : WidgetApiRepository {

    /**
     * OpenApi接口实现
     */
    private val openApi: OpenApi = Retrofit.Builder()
        .baseUrl(openUrl)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HeaderManageInterceptor(appId, userToken)
                ).build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenApi::class.java)

    /**
     * AppWidgetApi接口实现
     */
    private val appWidgetApi: AppWidgetApi = Retrofit.Builder()
        .baseUrl(aepUrl)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HeaderManageInterceptor(appId, userToken)
                ).build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AppWidgetApi::class.java)

    override suspend fun fetchUserDeviceList(): Result<UserDeviceList> {
        return runCatching {
            openApi.fetchUserDeviceList()
        }.onFailure { cancellationException ->
            if (cancellationException is CancellationException) {
                throw cancellationException
            }
        }
    }

    override suspend fun executeUserScene(homeId: Int, sceneId: Int): Result<WidgetApiResponse> {
        return runCatching {
            appWidgetApi.executeUserScene(homeId, sceneId)
        }.onFailure { cancellationException ->
            if (cancellationException is CancellationException) {
                throw cancellationException
            }
        }
    }

}















