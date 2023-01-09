package com.gizwitswidget.model

import com.google.gson.annotations.SerializedName

data class SceneConfiguration(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("homeId")
    val homeId: Int,
    @SerializedName("homeName")
    val homeName: String,
    @SerializedName("icon")
    val iconName: String
)
