package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Emote

class CustomEmoji(val name: String, val id: String) {

    val emote: Emote? by lazy {
        Bot.shardManager.shards.forEach {
            if (it.getEmoteById(id) != null)
                return@lazy it.getEmoteById(id)
        }
        null
    }

    override fun toString(): String {
        return "<:$name:$id>"
    }
}