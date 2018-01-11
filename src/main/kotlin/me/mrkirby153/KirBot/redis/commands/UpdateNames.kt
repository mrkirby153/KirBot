package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.utils.kirbotGuild
import org.json.JSONObject

class UpdateNames : RedisCommandHandler {

    override fun handle(json: JSONObject) {
        val server = json.getString("server")

        val guild = Bot.shardManager.getGuild(server) ?: return
        RealnameHandler(guild.kirbotGuild).update()
    }

}