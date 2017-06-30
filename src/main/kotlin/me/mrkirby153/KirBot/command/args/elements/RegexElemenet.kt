package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.CommandElement

class RegexElemenet(key: String, required: Boolean = true, val regex: String, override val friendlyName: String = "string"):
        CommandElement(key, required) {

    override fun parseValue(arg: String): Any {
        if(!arg.matches(Regex(regex))){
            throw ArgumentParseException("Invalid format for `$arg`")
        }
        return arg
    }
}