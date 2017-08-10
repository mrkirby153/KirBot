package me.mrkirby153.KirBot.database.api


import me.mrkirby153.KirBot.utils.HttpUtils
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.json.JSONTokener
import java.util.*

class ApiProcessor(private val API_ENDPOINT: String, private val apiKey: String) : Runnable {
    private val queue = LinkedList<ApiRequest<out ApiResponse>>()

    private var running = true

    fun executeNext() {
        if (queue.size < 1)
            return
        val apiRequest = queue.first

        val json = run(apiRequest)
        if (json != null) {
            apiRequest.execute(apiRequest.parse(json))
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

        val resp = HttpUtils.CLIENT.newCall(req).execute()

        if (resp.code() == 429) {
            // Hit API ratelimit
            val header = resp.header("Retry-After")
            if (header != null) {
                val retryAfter = Integer.parseInt(header)
                try {
                    Thread.sleep((1000 * retryAfter).toLong())
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
            val json = JSONObject(JSONTokener(inputStream))

            resp.close()
            return json
        }
        return null
    }

    override fun run() {
        while (running) {
            if (queue.size > 0) {
                executeNext()
            }
            Thread.yield()
        }
    }

    fun shutdown() {
        this.running = false
    }
}
