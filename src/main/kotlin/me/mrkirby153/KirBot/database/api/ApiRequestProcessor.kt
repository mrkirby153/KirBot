package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.HttpUtils
import net.dv8tion.jda.core.utils.SimpleLog
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.json.JSONTokener

class ApiRequestProcessor(val apiRequest: ApiRequest<*>) : Runnable {

    override fun run() {
        try {
            apiRequest.execute(Companion.run(apiRequest))
        } catch (e: Throwable) {
            if (Bot.debug)
                e.printStackTrace()
            Bot.LOG.fatal("Caught exception from request ${apiRequest.javaClass}: [$e]")
        }
    }

    companion object {

        private val debugLogger = SimpleLog.getLog("ApiProcessor")

        fun debug() {
            debugLogger.level = SimpleLog.Level.DEBUG
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

            debugLogger.debug("Making ${req.method()} to ${req.url()}")

            val resp = HttpUtils.CLIENT.newCall(req).execute()

            debugLogger.debug("Received code ${resp.code()}")

            if (resp.body() != null) {
                val inputStream = resp.body()!!.string()
                if (resp.code() != 200) {
                    apiRequest.onHttpError(resp.code(), inputStream);
                    return null
                }

                val json = if (inputStream.isNotBlank()) JSONObject(JSONTokener(inputStream)) else JSONObject()
                try {
                    return apiRequest.parse(json)
                } catch (e: Exception) {
                    apiRequest.onException(e)
                }
                resp.close()
            }
            return null
        }
    }
}