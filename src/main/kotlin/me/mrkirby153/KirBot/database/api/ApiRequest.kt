package me.mrkirby153.KirBot.database.api

import org.json.JSONObject

abstract class ApiRequest<out T : ApiResponse>(val url: String, val method: Methods = Methods.GET, val data: Map<String, String>? = null) {

    private var callback: ((T) -> Unit)? = null

    internal fun execute(response: ApiResponse) {
        this.callback?.invoke(response as T)
    }

    abstract fun parse(json: JSONObject): T

    fun queue(callback: ((T) -> Unit)? = null) {
        this.callback = callback
        PanelAPI.API_PROCESSOR.queue(this)
    }

    fun execute(): T {
        return PanelAPI.API_PROCESSOR.execute(this)
    }
}

interface ApiResponse

enum class Methods(val value: String) {
    GET("GET"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH"),
    POST("POST")
}