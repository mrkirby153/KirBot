package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject

class UpdateLogs : RedisCommandHandler {

    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        val id = json.getString("id")
        val included = json.getLong("included")
        val excluded = json.getLong("excluded")
        if(included == -1L && excluded == -1L) {
            guild?.kirbotGuild?.logManager?.deleteLogSettings(id)
        } else {
            guild?.kirbotGuild?.logManager?.updateLogSettings(id, included, excluded)
        }
    }
}