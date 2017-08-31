package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.utils.embed.ResponseBuilder
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Context(val author: User, val user: User, val channel: MessageChannel, val guild: Guild, val shard: Shard,
    val member: Member, val message: Message) {

    constructor(event: MessageReceivedEvent): this(event.author, event.author,
            event.channel, event.guild, Bot.getShardForGuild(event.guild.id)!!, event.member, event.message)

    val data: ServerData = Bot.getServerData(guild)!!
    fun send() = ResponseBuilder(this)
}