package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameHandler
import org.json.JSONObject

class UpdateNames : RedisCommandHandler {

    override fun handle(json: JSONObject) {
        val server = json.getString("server")

        val guild = Bot.getGuild(server) ?: return
        val data = Bot.getServerData(guild) ?: return
        RealnameHandler(guild, data).updateNames()
    }

}