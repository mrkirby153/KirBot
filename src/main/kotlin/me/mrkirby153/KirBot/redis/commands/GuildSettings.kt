package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.settings.SettingsRepository
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject

class SettingChange : RedisCommandHandler {
    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        if (guild == null) {
            Bot.LOG.warn("Changing settings for a guild that doesn't exist")
            return
        }

        var newval = json.get("value").toString()

        if (newval == "true" || newval == "false") {
            newval = if (newval == "true") "1" else "0"
        }
        SettingsRepository.onSettingsChange(guild.id, json.getString("key"),
                newval)
    }

}

class SettingDelete : RedisCommandHandler {
    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        if (guild == null) {
            Bot.LOG.warn("Deleting settings for a guild that doesn't exist")
            return
        }
        SettingsRepository.onSettingsChange(guild.id, json.getString("key"), null)
    }

}