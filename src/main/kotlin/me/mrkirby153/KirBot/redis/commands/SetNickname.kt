package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject

class SetNickname : RedisCommandHandler {
    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        if(guild == null)
            return
        val nick = json.optString("nickname")

        if (guild.selfMember.nickname != nick) {
            Bot.LOG.debug("Updating nick to \"$nick\"")
            guild.controller.setNickname(guild.selfMember, nick).queue()
        }
    }
}