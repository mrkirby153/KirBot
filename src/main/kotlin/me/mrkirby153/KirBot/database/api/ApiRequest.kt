package me.mrkirby153.KirBot.database.api

import org.json.JSONObject

abstract class ApiRequest<out T>(val url: String, val method: Methods = Methods.GET, val data: Map<String, String>? = null) {

    private var callback: ((T) -> Unit)? = null

    internal fun execute(response: Any?) {
        this.callback?.invoke(response as T)
    }

    abstract fun parse(json: JSONObject): T

    fun queue(callback: ((T) -> Unit)? = null) {
        this.callback = callback
        PanelAPI.executor.submit(ApiRequestProcessor(this))
    }

    fun execute(): T {
        return ApiRequestProcessor.process(this) as T
    }
}

enum class Methods(val value: String) {
    GET("GET"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH"),
    POST("POST")
}