package me.mrkirby153.KirBot.redis.commands

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject

interface RedisCommandHandler {

    fun handle(guild: Guild?, user: User?, json: JSONObject)
}