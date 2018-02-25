package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent

class Commands : Module("commands") {

    override fun onLoad() {
        CommandExecutor.loadAll()
        CommandExecutor.helpManager.load()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.author.isFake)
            Bot.seenStore.update(event.author, event.guild)

        if (event.author == Bot.shardManager.getShard(event.guild)!!.selfUser)
            return

        val context = Context(event)

        CommandExecutor.execute(context)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        // If the message was edited
        if (event.channel.latestMessageId == event.messageId) {
            // Only process if it's the last message
            if (event.author == Bot.shardManager.getShard(event.guild)!!.selfUser)
                return

            val context = Context(Bot.shardManager.getShard(event.guild)!!, event.message)
            CommandExecutor.execute(context)
        }
    }
}