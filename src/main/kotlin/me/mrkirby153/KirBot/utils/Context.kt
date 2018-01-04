package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.embed.ResponseBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Context(val shard: Shard, val message: Message) : Message by message {

    constructor(event: MessageReceivedEvent) : this(Bot.shardManager.getShard(event.guild)!!, event.message)

    var customAuthor: User? = null
    var customMessage : String? = null

    val data: ServerData = Bot.getServerData(guild)!!
    fun send() = ResponseBuilder(this)

    fun success() = message.addReaction(GREEN_CHECK).queue()

    fun fail() = message.addReaction(RED_CROSS).queue()

    override fun getAuthor(): User {
        return customAuthor?: message.author
    }

    override fun getRawContent(): String {
        return customMessage ?: message.rawContent
    }
}