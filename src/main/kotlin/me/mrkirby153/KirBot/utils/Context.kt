package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.utils.embed.ResponseBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

open class Context(val message: Message) : Message by message {

    constructor(event: MessageReceivedEvent) : this(event.message)

    val kirbotGuild
        get() = message.guild.kirbotGuild

    fun send() = ResponseBuilder(this)

    fun success() = message.addReaction(GREEN_TICK.emote).queue()

    fun fail() = message.addReaction(RED_TICK.emote).queue()
}