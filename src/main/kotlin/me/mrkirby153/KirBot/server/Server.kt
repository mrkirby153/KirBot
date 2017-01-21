package me.mrkirby153.KirBot.server

import me.mrkirby153.KirBot.command.CommandManager
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.managers.GuildManager

/**
 * Represent the bot
 */
class Server(guild: Guild) : GuildManager(guild), Guild by guild {

    fun handleMessageEvent(event: MessageReceivedEvent) {
        // Ignore PMs
        if (event.isFromType(ChannelType.PRIVATE))
            return
        CommandManager.call(event)
    }

    fun kick(user: String): Boolean {
        try {
            guild.controller.kick(user).queue()
            return true
        } catch (e: PermissionException) {
            return false
        }
    }

    fun ban(user: String): Boolean {
        try {
            guild.controller.ban(user, 2).queue()
            return true
        } catch (e: PermissionException){
            return false
        }
    }

    fun deleteMessage(msg: Message) : Boolean {
        try{
            msg.deleteMessage().queue()
            return true
        } catch (e: PermissionException){
            return false
        }
    }
}