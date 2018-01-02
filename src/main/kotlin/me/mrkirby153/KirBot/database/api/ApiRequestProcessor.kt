package me.mrkirby153.KirBot.database.api

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.HttpUtils
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.LoggerFactory

class ApiRequestProcessor(val apiRequest: ApiRequest<*>) : Runnable {

    override fun run() {
        try {
            val response = Companion.run(apiRequest) ?: return
            apiRequest.execute(response)
        } catch (e: Throwable) {
            if (Bot.debug)
                e.printStackTrace()
            Bot.LOG.error("Caught exception from request ${apiRequest.javaClass}: [$e]")
            debugLogger.debug(
                    "URL: ${apiRequest.url} (${apiRequest.method.value}); Data: ${apiRequest.data}")
        }
    }

    companion object {

        private val debugLogger = LoggerFactory.getLogger(ApiRequestProcessor::class.java)

        fun debug() {
//            debugLogger.level = SimpleLog.Level.DEBUG
            (debugLogger as? Logger)?.level = Level.DEBUG
        }

        fun run(apiRequest: ApiRequest<*>): Any? {
            val req = Request.Builder().run {
                url(PanelAPI.API_ENDPOINT + apiRequest.url)
                header("api-token", PanelAPI.API_KEY)
                header("Accept", "application/json")

                var body: RequestBody? = null
                if (apiRequest.data != null) {
                    body = FormBody.Builder().run {
                        apiRequest.data.forEach { (key, value) ->
                            add(key, value)
                        }
                        build()
                    }
                }
                method(apiRequest.method.value, body)

                build()
            }

            debugLogger.debug("{${apiRequest.apiReq}} Making ${req.method()} to ${req.url()}")

            val resp = HttpUtils.CLIENT.newCall(req).execute()

            debugLogger.debug("{${apiRequest.apiReq}} Received code ${resp.code()}")

            if (resp.body() != null) {
                val inputStream = resp.body()!!.string()
                if (resp.code() < 200 || resp.code() > 299) {
                    apiRequest.onHttpError(resp.code(), inputStream);
                    return null
                }

                val json = if (inputStream.isNotBlank()) JSONObject(
                        JSONTokener(inputStream)) else JSONObject()
                if (json.optBoolean("success", false)) {
                    val array = json.optJSONArray("data")
                    val obj = json.optJSONObject("data")
                    try {
                        if (array != null) {
                            return apiRequest.parse(array)
                        } else if (obj != null) {
                            return apiRequest.parse(obj)
                        } else {
                            return null
                        }
                    } catch (e: Exception) {
                        apiRequest.onException(e)
                    }
                } else {
                    debugLogger.debug("{${apiRequest.apiReq}} Request failed.")
                    return null
                }
                resp.close()
            }
            return null
        }
    }
}