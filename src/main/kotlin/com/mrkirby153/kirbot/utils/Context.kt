package com.mrkirby153.kirbot.utils

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 * Wrapper for [Message] which provides various helper methods and utilities
 */
class Context(private val message: Message) : Message by message {

    constructor(event: MessageReceivedEvent) : this(event.message)

    /**
     * Gets the response builder for use
     */
    fun reply() = ResponseBuilder(this.channel)
}

/**
 * Gets a [Context] for this message
 */
val Message.context
    get() = Context(this)

/**
 * Gets the [ResponseBuilder] for this channel
 */
val MessageChannel.responseBuilder
    get() = ResponseBuilder(this)