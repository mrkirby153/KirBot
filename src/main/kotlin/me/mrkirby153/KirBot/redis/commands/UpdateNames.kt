package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject

class UpdateNames : RedisCommandHandler {

    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        if (guild != null)
            RealnameHandler(guild.kirbotGuild).update()
    }

}