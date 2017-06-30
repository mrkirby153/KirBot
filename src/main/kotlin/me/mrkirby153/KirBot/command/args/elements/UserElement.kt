package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.CommandElement

class UserElement(key: String, required: Boolean) : CommandElement(key, required) {

    override val friendlyName = "Mention"

    override fun parseValue(arg: String): Any {
        val id = arg.replace("[<@!>]".toRegex(), "")
        return Bot.getUser(id) ?: throw ArgumentParseException("A user was not found with the id $id")
    }
}