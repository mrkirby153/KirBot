package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject

class SyncCommand : RedisCommandHandler {

    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        Bot.LOG.debug("Received Sync command from Panel for guild $guild")
        guild?.kirbotGuild?.loadSettings()
    }
}