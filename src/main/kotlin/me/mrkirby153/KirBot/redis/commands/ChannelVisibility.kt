package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.hide
import me.mrkirby153.KirBot.utils.unhide
import org.json.JSONObject

class ChannelVisibility : RedisCommandHandler {
    override fun handle(json: JSONObject) {
        val server = json.getString("server")
        val chan = json.getString("channel")
        val visible = json.getBoolean("visible")

        if (!visible)
            Bot.shardManager.getGuild(server)?.getTextChannelById(chan)?.hide()
        else
            Bot.shardManager.getGuild(server)?.getTextChannelById(chan)?.unhide()
    }

}