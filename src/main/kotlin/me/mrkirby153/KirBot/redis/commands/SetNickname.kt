package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import org.json.JSONObject

class SetNickname: RedisCommandHandler {
    override fun handle(json: JSONObject) {
        val server = json.getString("server")
        val nick = json.optString("nickname")

        Bot.getGuild(server)?.let { guild ->
            if(guild.selfMember.nickname != nick) {
                Bot.LOG.debug("Updating nick to \"$nick\"")
                guild.controller.setNickname(guild.selfMember, nick).queue()
            }
        }
    }
}