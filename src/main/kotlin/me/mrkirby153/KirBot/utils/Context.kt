package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.embed.ResponseBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

open class Context(val shard: Shard, val message: Message) : Message by message {

    constructor(event: MessageReceivedEvent) : this(Bot.shardManager.getShard(event.guild)!!,
            event.message)

    val kirbotGuild
        get() = message.guild.kirbotGuild

    fun send() = ResponseBuilder(this)

    fun success() = message.addReaction(GREEN_TICK.emote).queue()

    fun fail() = message.addReaction(RED_TICK.emote).queue()
}