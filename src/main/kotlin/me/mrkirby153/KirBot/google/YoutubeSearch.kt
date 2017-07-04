package me.mrkirby153.KirBot.google

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.utils.HttpUtils
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder

class YoutubeSearch(val query: String) {


    private val API_BASE_URL = "https://www.googleapis.com"

    private val SEARCH_ENDPOINT = "$API_BASE_URL/youtube/v3/search"

    private val API_KEY: String by lazy {
        Bot.properties.getProperty("google-api-key")
    }

    fun execute(): String {
        val request = Request.Builder().url(getQueryString()).build()

        val response = HttpUtils.CLIENT.newCall(request).execute()

        if(!response.isSuccessful)
            throw CommandException("Unexpected code $response")

        val jsonObject = JSONObject(JSONTokener(response.body()?.byteStream()))

        jsonObject.optJSONObject("error")?.let {
            throw CommandException(it.getString("message"))
        }

        val jsonArray = jsonObject.optJSONArray("items")
        if(jsonArray == null || jsonArray.count() == 0)
            throw CommandException("No items returned!")
        return jsonArray.getJSONObject(0).getJSONObject("id").getString("videoId")
    }

    private fun getQueryString(): String {
        return "$SEARCH_ENDPOINT?q=${URLEncoder.encode(query, "UTF-8")}&key=$API_KEY&part=snippet&maxResults=1"
    }
}