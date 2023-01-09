package com.gizwits.xb.model

import com.google.gson.annotations.SerializedName

data class AppWidgetConfiguration(
    @SerializedName("appKey")
    val appId: String,
    @SerializedName("uid")
    val userId: String,
    @SerializedName("token")
    val userToken: String,
    @SerializedName("openUrl")
    val openUrl: String,
    @SerializedName("m2mUrl")
    val m2mUrl: String,
    @SerializedName("m2mStageUrl")
    val m2mStageUrl: String,
    @SerializedName("aepUrl")
    val aepUrl: String,
    @SerializedName("tintColor")
    val tintColor: String,
    @SerializedName("languageKey")
    val languageKey: String
)
