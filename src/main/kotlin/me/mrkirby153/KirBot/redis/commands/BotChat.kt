package me.mrkirby153.KirBot.redis.commands

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject

class BotChat: RedisCommandHandler {
    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        val channel = json.getString("channel")
        val msg = json.getString("message")
        guild?.getTextChannelById(channel)?.sendMessage(msg)?.queue()
    }
}