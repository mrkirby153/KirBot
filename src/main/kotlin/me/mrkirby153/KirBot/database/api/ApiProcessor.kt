package me.mrkirby153.KirBot.database.api


import me.mrkirby153.KirBot.utils.HttpUtils
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.*

class ApiProcessor(private val API_ENDPOINT: String, private val apiKey: String) : Runnable {
    private val queue = LinkedList<ApiRequest<out ApiResponse>>()

    private var running = true

    fun executeNext() {
        if (queue.size < 1)
            return
        val apiRequest = queue.first

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

        try {
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
                    executeNext()
                    return
                }
            }


            if (resp.body() != null) {

                val inputStream = resp.body()!!.string()
                if (resp.code() != 200) {
                    System.err.println("An error occurred when accessing ${req.url()} (${resp.code()})")
                    System.err.println(inputStream)
                }
                val json = JSONObject(JSONTokener(inputStream))

                val data = apiRequest.parse(json)

                apiRequest.execute(data)
                resp.close()
                queue.removeFirst()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun queue(request: ApiRequest<*>) {
        this.queue.add(request)
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
