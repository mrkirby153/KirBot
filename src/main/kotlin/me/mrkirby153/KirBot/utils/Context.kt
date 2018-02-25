package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.embed.ResponseBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Context(val shard: Shard, val message: Message) : Message by message {

    constructor(event: MessageReceivedEvent) : this(Bot.shardManager.getShard(event.guild)!!,
            event.message)

    var customAuthor: User? = null
    var customMessage: String? = null

    val kirbotGuild
        get() = message.guild.kirbotGuild

    fun send() = ResponseBuilder(this)

    fun success() = message.addReaction(GREEN_TICK.emote).queue()

    fun fail() = message.addReaction(RED_TICK.emote).queue()

    override fun getAuthor(): User {
        return customAuthor ?: message.author
    }

    override fun getContentRaw(): String {
        return customMessage ?: message.contentRaw
    }
}