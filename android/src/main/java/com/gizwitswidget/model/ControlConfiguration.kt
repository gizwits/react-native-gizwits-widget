package com.gizwitswidget.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class ControlConfiguration {

    @SerializedName("name")
    val name: String? = null

    @SerializedName("icon")
    lateinit var icon: String

    @SerializedName("language")
    lateinit var language: JsonObject

    @SerializedName("productKey")
    lateinit var productKey: String

    @SerializedName("config")
    val configs: List<ControlConfig> = emptyList()

    @SerializedName("did")
    lateinit var deviceId: String

    @SerializedName("mac")
    lateinit var deviceMac: String

}

data class ControlConfig(
    @SerializedName("id")
    val id: String,
    @SerializedName("editName")
    val nameId: String,
    @SerializedName("attrsIcon")
    val attrsIcon: String,
    @SerializedName("sort")
    val sort: Int,
    @SerializedName("type")
    val type: String,
    @SerializedName("attrs")
    val attrsKey: String,
    @SerializedName("option")
    val options: List<ControlOption> = emptyList()
)

class ControlOption {

    @SerializedName("name")
    lateinit var nameId: String

    @SerializedName("image")
    lateinit var imageUrl: String

    @SerializedName("value")
    lateinit var value: JsonElement

    @SerializedName("notInOption")
    val notInOption: Boolean = false

}

















