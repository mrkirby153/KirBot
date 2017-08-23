package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
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

    fun execute(): T? {
        return ApiRequestProcessor.run(this) as? T
    }

    open fun onException(e: Exception){
        e.printStackTrace()
    }

    open fun onHttpError(error: Int, body: String){
        Bot.LOG.fatal("[HTTP ERROR] Encountered an error ($error) when accessing \"$url\"")
        Bot.LOG.fatal("[HTTP ERROR] $body")
    }
}

enum class Methods(val value: String) {
    GET("GET"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH"),
    POST("POST")
}