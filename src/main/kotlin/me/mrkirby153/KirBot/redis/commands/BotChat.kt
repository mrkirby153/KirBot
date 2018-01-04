package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import org.json.JSONObject

class BotChat: RedisCommandHandler {
    override fun handle(json: JSONObject) {
        val server = json.getString("server")
        val channel = json.getString("channel")
        val msg = json.getString("message")
        Bot.shardManager.getShard(server)?.getTextChannelById(channel)?.sendMessage(msg)?.queue()
    }
}