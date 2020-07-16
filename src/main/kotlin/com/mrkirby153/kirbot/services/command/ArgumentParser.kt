package com.mrkirby153.kirbot.services.command

import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException

@Service
class ArgumentParser : ArgumentParserService {

    private val requiredPattern = Regex("^<.*>$")
    private val optionalPattern = Regex("^\\[.*]$")

    override fun parseArguments(node: CommandNode): List<ArgumentParserService.CommandArgument> {
        val args = mutableListOf<ArgumentParserService.CommandArgument>()
        node.annotation.arguments.forEach { arg ->
            val type: ArgumentParserService.ArgType = when {
                requiredPattern.matches(arg) -> ArgumentParserService.ArgType.REQUIRED
                optionalPattern.matches(arg) -> ArgumentParserService.ArgType.OPTIONAL
                else -> throw IllegalArgumentException("Argument \"$arg\" is not in the right format")
            }
            val param = node.commandParameters[arg] ?: throw IllegalArgumentException("Could not find command parameter for $arg")
            args.add(ArgumentParserService.CommandArgument(type, arg.replace(Regex("[<>\\[\\]]"), ""), param))
        }
        return args
    }
}