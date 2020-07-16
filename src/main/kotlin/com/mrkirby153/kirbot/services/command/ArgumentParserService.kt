package com.mrkirby153.kirbot.services.command

import com.mrkirby153.kirbot.services.command.context.CommandParameter

/**
 * Service responsible for translating arguments to command nodes
 */
interface ArgumentParserService {

    /**
     * Translate command node's arguments into a list of [CommandArgument]
     *
     * @param node The node to translate
     * @return A list of command arguments
     */
    fun parseArguments(node: CommandNode): List<CommandArgument>

    data class CommandArgument(val type: ArgType, val name: String, val param: CommandParameter)
    enum class ArgType {
        REQUIRED,
        OPTIONAL
    }
}