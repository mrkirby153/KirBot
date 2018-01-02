package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import org.json.JSONArray
import org.json.JSONObject

var apiReqCounter = 0L

abstract class ApiRequest<out T>(val url: String, val method: Methods = Methods.GET,
                                 val data: Map<String, String>? = null) {

    val apiReq = ++apiReqCounter

    private var callback: ((T) -> Unit)? = null

    internal fun execute(response: Any?) {
        this.callback?.invoke(response as T)
    }

    open fun parse(json: JSONObject): T? {
        return null
    }

    open fun parse(json: JSONArray): T? {
        return null
    }

    fun queue(callback: ((T) -> Unit)? = null) {
        this.callback = callback
        PanelAPI.executor.submit(ApiRequestProcessor(this))
    }

    fun execute(): T? {
        return ApiRequestProcessor.run(this) as? T
    }

    open fun onException(e: Exception) {
        e.printStackTrace()
    }

    open fun onHttpError(error: Int, body: String) {
        Bot.LOG.debug("{$apiReq} [HTTP ERROR] Encountered an error ($error) when accessing \"$url\"")
        Bot.LOG.debug("{$apiReq} [HTTP ERROR] $body")
    }
}

enum class Methods(val value: String) {
    GET("GET"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH"),
    POST("POST")
}