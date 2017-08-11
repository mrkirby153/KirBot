package me.mrkirby153.KirBot.listener

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.database.api.PanelAPI
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
                        return PanelAPI.guildSettings(Bot.getGuild(key)!!).execute().logChannel
                    }
                }
        )
    }


    override fun onMessageDelete(event: MessageDeleteEvent) {
        PanelAPI.deleteMessage(event.messageId).queue { msg ->
            if (msg.id == "-1")
                return@queue
            val author = msg.author
            val chan = msg.channel

            if (author != null && author.isBot)
                return@queue

            val authorMsg = if (author != null) "by `${author.name}#${author.discriminator}`" else ""
            shard.getServerData(event.guild).logger
                    .log("Message Deleted ",
                            "A message $authorMsg was deleted in `#${chan?.name}`", Color.BLUE,
                            LogField("Message", msg.content, true))
        }
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        PanelAPI.bulkDelete(event.messageIds).queue()
        shard.getServerData(event.guild).logger
                .log("Message Deleted", "`${event.messageIds.size}` messages have been deleted from #${event.channel.name}", Color.RED)
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        PanelAPI.editMessage(event.message).queue { msg ->
            if(msg.id == "-1")
                return@queue
            val user = msg.author ?: return@queue
            if (user.isBot)
                return@queue
            shard.getServerData(event.guild).logger
                    .log("Message Edit", "${user.name}#${user.discriminator} has edited their message",
                            Color.BLUE,
                            LogField("Old", "```${msg.content}```", false), LogField("New", "```${event.message.content}```", false))
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.channel.id == logChannelCache[event.guild.id])
            return
        PanelAPI.logMessage(event.message).queue()
    }
}