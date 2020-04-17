package me.mrkirby153.KirBot.command.args

import me.mrkirby153.KirBot.Bot

class ArgumentParser(private val contextResolvers: ContextResolvers, val arguments: Array<String>) {

    init {
        Bot.LOG.debug(
                "Constructing argument parser with arguments: ${arguments.joinToString(
                        ", ") { "\'$it\'" }}")
    }

    fun parse(args: Array<String>): CommandContext {
        Bot.LOG.debug("Beginning parse of arguments ${arguments.joinToString(",")}")
        val argList = ArgumentList(arguments)
        val context = CommandContext()
        // "name:type,paramOne,paramTwo,param3"

        for (i in 0 until args.size) {
            var argument = args[i]
            Bot.LOG.debug("Beginning parse of \"$argument\"")
            val argType = ArgType.determine(argument)
            Bot.LOG.debug("\tArg type $argType")
            argument = argument.replace(Regex("<|>|\\[|]"), "")
            val parts = argument.split(":")
            val name = parts[0]
            val type = parts[1].split(",")[0]
            if (argType == ArgType.REQUIRED && !argList.hasNext()) {
                Bot.LOG.debug("Missing required argument")
                throw ArgumentParseException("The argument `<$name:$type>` is required!")
            }
            if (argList.hasNext()) {
                    val parse = contextResolvers.getResolver(type)?.invoke(argList)
                    context.put(name, parse)
            }
        }
        return context
    }


    private enum class ArgType {
        UNKNOWN,
        REQUIRED,
        OPTIONAL;

        companion object {

            fun determine(arg: String): ArgType {
                val requiredPattern = Regex("^<.*>$")
                val optionalPattern = Regex("^\\[.*]$")

                return when {
                    optionalPattern.matches(arg) -> OPTIONAL
                    requiredPattern.matches(arg) -> REQUIRED
                    else -> UNKNOWN
                }
            }
        }
    }
}