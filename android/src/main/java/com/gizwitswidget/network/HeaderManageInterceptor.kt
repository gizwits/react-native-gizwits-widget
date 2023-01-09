package com.gizwits.xb.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class HeaderManageInterceptor(
    val appId: String,
    val authorization: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 向请求添加所需的请求头
        val nativeRequestBuilder: Request.Builder = chain.request().newBuilder()
        nativeRequestBuilder.addHeader("X-Gizwits-Application-Id", appId)
        nativeRequestBuilder.addHeader("Authorization", authorization)
        nativeRequestBuilder.addHeader("X-Gizwits-User-token", authorization)
        return chain.proceed(nativeRequestBuilder.build())
    }

}
