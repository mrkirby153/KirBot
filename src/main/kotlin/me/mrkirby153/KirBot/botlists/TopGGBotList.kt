package me.mrkirby153.KirBot.botlists

import me.mrkirby153.KirBot.Bot
import org.discordbots.api.client.DiscordBotListAPI

class TopGGBotList(token: String, private val botId: String) : BotList {

    private val api = DiscordBotListAPI.Builder().token(token).botId(botId).build()

    override fun updateBotlist(count: Int) {
        Bot.LOG.debug("Updating top.gg bot $botId with $count servers")
        api.setStats(count).handle { throwable, _ ->
            if(throwable != null) {
                Bot.LOG.warn("Could not update top.gg", throwable)
            } else {
                Bot.LOG.debug("Updated top.gg successfully")
            }
        }
    }
}