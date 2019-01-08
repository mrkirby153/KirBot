package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.realname.RealnameUpdater
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject

class UpdateAllServerNames : RedisCommandHandler {

    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        RealnameUpdater().run()
    }
}