package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.utils.embed.ResponseBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Context(val event: MessageReceivedEvent) {
    val author: User = event.author
    val user: User = event.author
    val channel: MessageChannel = event.channel
    val guild: Guild = event.guild
    val data: ServerData = Bot.getServerData(guild)!!
    val shard: Shard = data.shard

    val member = event.member

    val message = event.message

    fun send() = ResponseBuilder(this)
}