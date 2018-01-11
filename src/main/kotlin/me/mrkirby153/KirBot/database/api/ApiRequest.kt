package me.mrkirby153.KirBot.database.api

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.logger.ErrorLogger
import me.mrkirby153.KirBot.utils.HttpUtils
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

var apiReqCounter = 0L

private val apiLogger = LoggerFactory.getLogger("ApiRequestProcessor")
private val executor = Executors.newFixedThreadPool(3,
        ThreadFactoryBuilder().setNameFormat("ApiRequestThread-%d").build())

fun enableApiDebug() {
    (apiLogger as? Logger)?.level = Level.DEBUG
    apiLogger.debug("Debug logging enabled!")
}

abstract class ApiRequest<T>(val url: String, val method: Methods = Methods.GET,
                             val data: Map<String, String>? = null) : Callable<T> {

    private val apiReq = ++apiReqCounter

    private var callback: ((T) -> Unit)? = null

    open fun parse(json: JSONObject): T? {
        return null
    }

    open fun parse(json: JSONArray): T? {
        return null
    }

    open fun onException(e: Exception) {
        e.printStackTrace()
    }

    fun queue(callback: ((T) -> Unit)? = null): Future<T> {
        this.callback = callback
        return executor.submit(this)
    }

    fun execute(timeout: Long = 1, unit: TimeUnit = TimeUnit.SECONDS): T {
        return queue(null).get(timeout, unit)
    }

    open fun onHttpError(error: Int, body: String) {
        Bot.LOG.debug(
                "{$apiReq} [HTTP ERROR] Encountered an error ($error) when accessing \"$url\"")
        Bot.LOG.debug("{$apiReq} [HTTP ERROR] $body")
    }

    override fun call(): T? {
        val req = Request.Builder().run {
            url(PanelAPI.API_ENDPOINT + this@ApiRequest.url)
            header("api-token", PanelAPI.API_KEY)
            header("Accept", "application/json")

            var body: RequestBody? = null
            if (this@ApiRequest.data != null) {
                body = FormBody.Builder().run {
                    this@ApiRequest.data.forEach { (key, value) ->
                        add(key, value)
                    }
                    build()
                }
            }
            method(method.value, body)
            build()
        }

        apiLogger.debug("{$apiReq} Making ${req.method()} to ${req.url()}")
        data?.forEach { key, value ->
            apiLogger.debug("    \"$key\" = \"$value\"")
        }

        val resp = HttpUtils.CLIENT.newCall(req).execute()
        apiLogger?.debug("{$apiReq} Received code ${resp.code()}")

        if (resp.body() != null) {
            val body = resp.body()!!.string()
            // Check if we get an HTTP code in the success range
            if (resp.code() < 200 || resp.code() > 299) {
                onHttpError(resp.code(), body)
                return null
            }

            val json = if (body.isNotBlank()) JSONObject(JSONTokener(body)) else JSONObject()

            if (json.optBoolean("success", false)) {
                val array = json.optJSONArray("data")
                val obj = json.optJSONObject("data")
                try {
                    val data = when {
                        array != null -> parse(array)
                        obj != null -> parse(obj)
                        else -> null
                    }
                    if (data == null && callback != null) {
                        apiLogger.warn(
                                "{$apiReq} parse() returned null, but the callback is not null. The callback will NOT be executed")
                    }
                    if (data != null)
                        callback?.invoke(data)
                    return data
                } catch (e: Exception) {
                    ErrorLogger.logThrowable(e)
                    onException(e)
                }
            } else {
                apiLogger.debug("{$apiReq} Request failed.")
                apiLogger.debug("{$apiReq} Body: $body")
            }
        }
        return null
    }
}

enum class Methods(val value: String) {
    GET("GET"),
    PUT("PUT"),
    DELETE("DELETE"),
    PATCH("PATCH"),
    POST("POST")
}