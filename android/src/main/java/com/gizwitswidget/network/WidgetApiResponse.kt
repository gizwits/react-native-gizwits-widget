package com.gizwitswidget.network

import com.google.gson.annotations.SerializedName

open class WidgetApiResponse {

    @SerializedName("code")
    val responseCode: String = "200"

    fun isSuccess(): Boolean = responseCode == "200"

}

data class UserSceneList(
    val data: List<UserScene>
) : WidgetApiResponse()

data class UserScene(
    @SerializedName("id")
    val sceneId: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("homeId")
    val homeId: Int
)

open class OpenApiResponse {

    @SerializedName("error_code")
    val errorCode: Int = 0

    @SerializedName("error_message")
    val errorMessage: String = ""

    fun isSuccess(): Boolean = errorCode == 0

}

data class UserDeviceList(
    @SerializedName("devices")
    val devices: List<UserDevice>
) : OpenApiResponse()

data class UserDevice(
    @SerializedName("did")
    val deviceId: String,
    @SerializedName("product_name")
    val deviceName: String,
    @SerializedName("is_online")
    val isOnline: Boolean,
    @SerializedName("is_sandbox")
    val isSandbox: Boolean
)

