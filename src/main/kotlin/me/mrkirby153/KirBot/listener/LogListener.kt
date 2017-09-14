package me.mrkirby153.KirBot.listener

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.database.api.GuildSettings
import me.mrkirby153.KirBot.server.LogField
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color
import java.util.concurrent.TimeUnit

class LogListener(private val shard: Shard) : ListenerAdapter() {

    companion object {
        val logChannelCache = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).build(
                object : CacheLoader<String, String?>() {
                    override fun load(key: String): String? {
                        return GuildSettings.get(Bot.getGuild(key)!!).execute()?.logChannel
                    }
                }
        )
    }


    override fun onMessageDelete(event: MessageDeleteEvent) {
        Bot.messageDataStore.getMessageContent(event.messageId, { msg ->
            if(msg == null)
                return@getMessageContent
            if(msg.id == "-1")
                return@getMessageContent

            val author = Bot.getUser(msg.author)
            val chan = Bot.getGuild(msg.serverId)?.getTextChannelById(msg.channel) ?: return@getMessageContent

            if(author != null && author.isBot)
                return@getMessageContent

            val authorMsg = if (author != null) "by `${author.name}#${author.discriminator}`" else ""
            shard.getServerData(event.guild).logger
                    .log("Message Deleted ",
                            "A message $authorMsg was deleted in `#${chan.name}`", Color.BLUE,
                            LogField("Message", msg.message, true))
            Bot.messageDataStore.deleteMessage(event.messageId, {})
        })
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        Bot.messageDataStore.bulkDelete(event.messageIds.toTypedArray(), {
            shard.getServerData(event.guild).logger
                    .log("Message Deleted", "`${event.messageIds.size}` messages have been deleted from #${event.channel.name}", Color.RED)
        })
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if(event.message.content.isEmpty())
            return
        Bot.messageDataStore.getMessageContent(event.message.id, { old ->
            if(old != null){
                if(old.id == "-1")
                    return@getMessageContent
                val user = Bot.getUser(old.author) ?: return@getMessageContent
                if(user.isBot)
                    return@getMessageContent
                shard.getServerData(event.guild).logger
                        .log("Message Edit", "${user.name}#${user.discriminator} has edited their message",
                                Color.BLUE,
                                LogField("Old", "```${old.message}```", false), LogField("New", "```${event.message.content}````", false))
            }
        })
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.channel.id == logChannelCache[event.guild.id])
            return
        if (event.message.content.isNotEmpty()) {
            Bot.messageDataStore.pushMessage(event.message)
        }
    }
}