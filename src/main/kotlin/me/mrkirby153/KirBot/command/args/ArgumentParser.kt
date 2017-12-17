package me.mrkirby153.KirBot.command.args

import me.mrkirby153.KirBot.command.CommandException

class ArgumentParser(val arguments: Array<String>) {

    fun parse(args: Array<Argument>): CommandContext {
        val argList = ArgumentList(arguments)
        val context = CommandContext()

        for (i in 0 until args.size) {
            val argument = args[i]
            if (argument.required && !argList.hasNext()) {
                // Missing a required argument
                throw CommandException("The argument \"${argument.key}\" is required!")
            }
            val parse = argument.element.parse(argList)!!
            context.put(argument.key, parse)
        }
        return context
    }
}