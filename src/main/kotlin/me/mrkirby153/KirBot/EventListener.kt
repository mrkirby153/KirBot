package me.mrkirby153.KirBot

import me.mrkirby153.KirBot.server.ServerRepository
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class EventListener : ListenerAdapter() {


    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event!!.author == Bot.jda.selfUser)
            return
        val server = ServerRepository.getServer(event.guild) ?: return
        server.handleBridge(event)
            server.handleMessageEvent(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent?) {
        ServerRepository.servers.remove(event?.guild?.id)
    }
}