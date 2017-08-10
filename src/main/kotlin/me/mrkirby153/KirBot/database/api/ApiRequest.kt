package me.mrkirby153.KirBot.database.api

import org.json.JSONObject

abstract class ApiRequest<out T : ApiResponse>(val url: String, val method: Methods = Methods.GET, val data: Map<String, String>? = null) {

    private var callback: ((T) -> Unit)? = null

    internal var processor: ApiProcessor? = null

    internal fun execute(response: ApiResponse) {
        this.callback?.invoke(response as T)
    }

    abstract fun parse(json: JSONObject): T

    fun queue(callback: (T) -> Unit) {
        if (processor == null) {
            throw IllegalStateException("An API processor has not been set for " + javaClass)
        }
        this.callback = callback
        this.processor?.queue(this)
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