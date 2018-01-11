package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.kirbotGuild
import org.json.JSONObject

class SyncCommand : RedisCommandHandler {

    override fun handle(json: JSONObject) {
        val id = json.getString("guild")
        Bot.LOG.debug("Received Sync command from Panel for guild $id")
        Bot.shardManager.getGuild(id)?.kirbotGuild?.sync()
    }
}