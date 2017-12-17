package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.command.args.ArgumentList
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.CommandElement

class RegexElement(private val regex: String, private val errorMessage: String? = null) : CommandElement<String> {

    override fun parse(list: ArgumentList): String {
        val arg = list.popFirst()
        if (!arg.matches(Regex(regex))) {
            throw ArgumentParseException(errorMessage ?: "`$arg` is not in the required format!")
        }
        return arg
    }
}