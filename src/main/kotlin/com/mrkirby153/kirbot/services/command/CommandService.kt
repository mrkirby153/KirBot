package com.mrkirby153.kirbot.services.command

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
     * @args The command to execute
     */
    fun executeCommand(args: String)
}