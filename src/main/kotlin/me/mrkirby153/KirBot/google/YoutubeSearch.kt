package me.mrkirby153.KirBot.google

import com.google.gson.JsonParser
import me.mrkirby153.KirBot.Bot
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

class YoutubeSearch(val query: String) {


    private val API_BASE_URL = "https://www.googleapis.com"

    private val SEARCH_ENDPOINT = "$API_BASE_URL/youtube/v3/search"

    private val API_KEY: String by lazy {
        Bot.properties.getProperty("google-api-key")
    }

    fun execute(): String {
        val url = URL(getQueryString())
        val conn = url.openConnection()


        val inputStream = conn.getInputStream()

        val jsonObj = JsonParser().parse(inputStream.reader(Charset.defaultCharset())).asJsonObject

        if (jsonObj.get("error") != null) {
            throw Exception(jsonObj.getAsJsonObject("error").get("message").asString)
        }

        val jsonItems = jsonObj.getAsJsonArray("items")

        return jsonItems[0].asJsonObject.getAsJsonObject("id").get("videoId").asString
    }

    private fun getQueryString(): String {
        return "$SEARCH_ENDPOINT?q=${URLEncoder.encode(query, "UTF-8")}&key=$API_KEY&part=snippet&maxResults=1"
    }
}