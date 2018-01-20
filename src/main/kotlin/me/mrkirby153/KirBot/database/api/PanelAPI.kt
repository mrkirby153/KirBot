package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import java.util.concurrent.Executors

object PanelAPI {

    internal val API_ENDPOINT = Bot.properties.getProperty("api-endpoint")
    internal val API_KEY = Bot.properties.getProperty("api-key")

    internal val executor = Executors.newFixedThreadPool(3)

}