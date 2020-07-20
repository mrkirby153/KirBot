package com.mrkirby153.kirbot.events

import com.mrkirby153.kirbot.services.command.CommandNode
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User

/**
 * Event fired when a command is executed
 */
data class CommandExecutedEvent(
        /**
         * The command that was executed
         */
        val command: CommandNode,
        /**
         * The arguments that the command was executed with
         */
        val args: List<String>,
        /**
         * The user executing the command
         */
        val user: User,
        /**
         * The guild that the command was executed on
         */
        val guild: Guild)