package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.server.LogField
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.MessageDeleteEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color

class LogListener(private val shard: Shard) : ListenerAdapter() {


    override fun onMessageDelete(event: MessageDeleteEvent) {
        val msg = Database.deleteMessage(event.messageId)
        if (msg != null) {
            val author = shard.getUserById(msg.author)
            val chan = shard.getTextChannelById(msg.channel)

            if (author != null && author.isBot)
                return

            val authorMsg = if (author != null) "by `${author.name}#${author.discriminator}`" else ""
            shard.getServerData(event.guild).logger
                    .log("Message Deleted ",
                            "A message $authorMsg was deleted in `#${chan.name}`", Color.BLUE,
                            LogField("Message", msg.message, true))
        }
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        val msgs = mutableListOf<Database.LogMessage>()
        event.messageIds.forEach { it ->
            val msg = Database.deleteMessage(it)
            if (msg != null)
                msgs.add(msg)
        }
        shard.getServerData(event.guild).logger
                .log("Message Deleted", "`${msgs.size}` messages have been deleted from #${event.channel.name}", Color.RED)
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        val msg = Database.editMessage(event.message)
        if (msg != null) {
            val user = shard.getUserById(msg.author)
            if (user.isBot)
                return
            shard.getServerData(event.guild).logger
                    .log("Message Edit", "${user.name}#${user.discriminator} has edited their message",
                            Color.BLUE,
                            LogField("Old", "```${msg.message}```", false), LogField("New", "```${event.message.content}```", false))
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        var logChan = shard.getServerData(event.guild).logger.channel.get()
        if (logChan == null)
            logChan = Database.getLoggingChannel(event.guild)
        if (event.channel.id == logChan)
            return
        Database.logMessage(event.message)
    }
}