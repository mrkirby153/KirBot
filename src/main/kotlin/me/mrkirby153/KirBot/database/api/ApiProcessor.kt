package me.mrkirby153.KirBot.database.api


import me.mrkirby153.KirBot.utils.HttpUtils
import net.dv8tion.jda.core.utils.SimpleLog
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*

class ApiProcessor(private val API_ENDPOINT: String, private val apiKey: String, private val debug: Boolean = false) : Runnable {
    private val queue = LinkedList<ApiRequest<out ApiResponse>>()

    private var running = true

    private var debugLogger = SimpleLog.getLog("ApiProcessor")

    init {
        if (debug)
            debugLogger.level = SimpleLog.Level.DEBUG
    }

    fun executeNext() {
        if (queue.size < 1)
            return
        val apiRequest = queue.first

        val json = run(apiRequest)
        if (json != null) {
            apiRequest.execute(apiRequest.parse(json))
            queue.removeFirst()
        } else {
            apiRequest.execute(apiRequest.parse(JSONObject()))
            queue.removeFirst()
        }
    }

    fun queue(request: ApiRequest<*>) {
        this.queue.add(request)
    }

    fun <T : ApiResponse> execute(request: ApiRequest<T>): T {
        val json = run(request) ?: throw IllegalStateException("Received null json object?")
        return request.parse(json)
    }

    private fun run(apiRequest: ApiRequest<*>): JSONObject? {
        val req = Request.Builder().run {
            url(API_ENDPOINT + apiRequest.url)
            header("api-token", apiKey)


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
        if (resp.code() == 429) {
            // Hit API ratelimit
            val header = resp.header("Retry-After")
            if (header != null) {
                val retryAfter = Integer.parseInt(header)
                try {
                    debugLogger.debug("Retrying after ${retryAfter} seconds")
                    Thread.sleep(((1000 * retryAfter) + 250).toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }


        if (resp.body() != null) {

            val inputStream = resp.body()!!.string()
            if (resp.code() != 200) {
                System.err.println("An error occurred when accessing ${req.url()} (${resp.code()})")
                System.err.println(inputStream)
            }
            if (inputStream.isNotBlank()) {
                val json = JSONObject(JSONTokener(inputStream))

                resp.close()
                return json
            }
        }
        return null
    }

    override fun run() {
        while (running) {
            if (queue.size > 0) {
                try {
                    executeNext()
                } catch(e: Exception) {
                    debugLogger.fatal("Caught exception when processing message!")
                    e.printStackTrace()
                    queue.removeFirst()
                }
            }
            Thread.yield()
        }
    }

    fun shutdown() {
        this.running = false
    }
}
