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


class ControlConfig {

    @SerializedName("id")
    lateinit var id: String

    @SerializedName("editName")
    lateinit var nameId: String

    @SerializedName("attrsIcon")
    lateinit var attrsIcon: String

    @SerializedName("type")
    lateinit var type: String

    @SerializedName("attrs")
    lateinit var attrsKey: String

    @SerializedName("option")
    val options: List<ControlOption> = emptyList()

}

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

















