package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.data
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class LogListener : ListenerAdapter() {

    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        event.guild.data().logManager.logMessageDelete(event.messageId)
        Bot.messageDataStore.deleteMessage(event.messageId, {})
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        event.guild.data().logManager.logBulkDelete(event.channel, event.messageIds.size)
        Bot.messageDataStore.bulkDelete(event.messageIds.toTypedArray(), {})
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.message.contentDisplay.isEmpty())
            return
        event.guild.data().logManager.logEdit(event.message)
        Bot.messageDataStore.pushMessage(event.message)
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.channel.id == event.guild.data().logManager.logChannel?.id) {
            return
        }
        if (event.message.contentDisplay.isNotEmpty())
            Bot.messageDataStore.pushMessage(event.message)
    }
}