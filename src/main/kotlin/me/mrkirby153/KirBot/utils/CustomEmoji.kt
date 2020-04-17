package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.sharding.ShardManager

class CustomEmoji(val name: String, val id: String) {

    val emote: Emote? by lazy {
        Bot.applicationContext.get(ShardManager::class.java).shards.forEach {
            if (it.getEmoteById(id) != null)
                return@lazy it.getEmoteById(id)
        }
        null
    }

    override fun toString(): String {
        return "<:$name:$id>"
    }
}