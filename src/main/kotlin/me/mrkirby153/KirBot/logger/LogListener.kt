package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class LogListener : ListenerAdapter() {

    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        event.guild.kirbotGuild.logManager.logMessageDelete(event.messageId)
        Model.first(GuildMessage::class.java, Pair("id", event.messageId))?.delete()
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        event.guild.kirbotGuild.logManager.logBulkDelete(event.channel, event.messageIds.size)
        val query = "DELETE FROM `server_messages` WHERE `id` IN (${event.messageIds.joinToString(
                ",") { "'$it'" }})"
        Bot.database.getConnection().use { conn ->
            conn.prepareStatement(query).use { ps ->
                ps.execute()
            }
        }
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.message.contentDisplay.isEmpty())
            return
        event.guild.kirbotGuild.logManager.logEdit(event.message)
        val msg = Model.first(GuildMessage::class.java, Pair("id", event.messageId)) ?: return
        msg.message = event.message.contentDisplay
        msg.save()
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.channel.id == event.guild.kirbotGuild.logManager.logChannel?.id) {
            return
        }
        if (event.message.contentDisplay.isNotEmpty()) {
            val msg = GuildMessage()
            msg.id = event.message.id
            msg.serverId = event.guild.id
            msg.author = event.author.id
            msg.channel = event.channel.id
            msg.message = event.message.contentDisplay
            msg.save()
        }
    }
}