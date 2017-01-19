package me.mrkirby153.KirBot

import me.mrkirby153.KirBot.command.CommandManager
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class EventListener : ListenerAdapter() {


    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event == null)
            return
        var rawMessage = event.message.rawContent

        if (!rawMessage.startsWith("!"))
            return

        rawMessage = rawMessage.substring(1)
        val parts: List<String> = rawMessage.split(" ")

        val command = parts[0]

        val args = if (parts.isNotEmpty()) parts.drop(1) else parts

        val executor = CommandManager.commands[command.toLowerCase()]

        // TODO 1/18/2017 Implement arguments and channel to commands
        executor?.execute()
    }
}