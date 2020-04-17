package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import javax.inject.Inject


class Commands @Inject constructor(private val commandExecutor: CommandExecutor) : Module("commands") {

    override fun onLoad() {
        commandExecutor.loadAll()
    }

    @Subscribe
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author == event.jda.selfUser)
            return

        val context = Context(event)

        commandExecutor.executeAsync(context)
    }

    @Subscribe
    fun onMessageUpdate(event: MessageUpdateEvent) {
        // If the message was edited
        if (event.channel.hasLatestMessage())
            if (event.channel.latestMessageId == event.messageId) {
                // Only process if it's the last message
                if (event.author == event.jda.selfUser)
                    return

                val context = Context(event.message)
                commandExecutor.executeAsync(context)
            }
    }
}