package com.mrkirby153.kirbot.services.command

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User

/**
 * Service for handling the registration and execution of commands
 */
interface CommandService {

    /**
     * Registers a command into the command context. The class will automatically have its methods
     * scanned for the [Command] annotation and its methods registered
     *
     * @param clazz The object to register
     * @throws IllegalArgumentException If a method in the class has already been found to exist
     */
    fun registerCommand(clazz: Any)

    /**
     * Gets a command by its argument chain. The command tree will be traversed until a match was found and
     * the command object returned
     *
     * @param args The arguments for the command
     * @return The command info for the command
     */
    fun getCommand(args: String): CommandNode?

    /**
     * Gets a command by its argument chain. The command tree will be traversed until a match was found
     * and the command object returned
     *
     * @param args An array of arguments for the command
     * @return The command info for the command
     */
    fun getCommand(args: List<String>): CommandNode?

    /**
     * Executes a command
     *
     * @param message The command to execute
     * @param user The user executing the command
     * @param channel The channel the command is being executed in
     */
    fun executeCommand(message: String, user: User, channel: MessageChannel)

    /**
     * Invokes a command node
     *
     * @param node The command node
     * @param args The arguments to invoke
     * @param user The user invoking the command
     * @param guild The guild that the command is being invoked on
     * @param channel The channel the command is being executed in
     */
    fun invoke(node: CommandNode, args: List<String>, user: User, guild: Guild?, channel: TextChannel)

    /**
     * Gets the usage string for a command
     *
     * @param node The node to get the usage string for
     * @return The usage string
     */
    fun getUsageString(node: CommandNode): String
}