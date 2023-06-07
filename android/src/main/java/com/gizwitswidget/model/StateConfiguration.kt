package com.gizwitswidget.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class StateConfiguration {

    @SerializedName("id")
    val id: Int = 0

    @SerializedName("did")
    lateinit var deviceId: String

    @SerializedName("attrs")
    lateinit var attrsKey: String

    @SerializedName("editName")
    lateinit var editName: String

    @SerializedName("title")
    val title: StateTitle? = null

    @SerializedName("icon")
    val icon: String? = null

    @SerializedName("language")
    lateinit var language: JsonObject

    @SerializedName("type")
    lateinit var type: String

    @SerializedName("content")
    val contentList: List<StateContent> = emptyList()

}

class StateTitle {

    @SerializedName("text")
    lateinit var textId: String

}

class StateContent {

    @SerializedName("text")
    val text: String? = null

    @SerializedName("image")
    val image: String? = null

    @SerializedName("formatTitle")
    val formatTitle: StateContentFormatTitle? = null

    @SerializedName("conditions")
    val conditions: List<StateContentCondition> = emptyList()

}

class StateContentCondition {

    @SerializedName("opt")
    lateinit var operation: String

    @SerializedName("value")
    lateinit var value: JsonElement

}

class StateContentFormatTitle {

    @SerializedName("type")
    lateinit var type: String

}

