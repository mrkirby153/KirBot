package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.command.args.CommandElement

class RestToString(key: String, override val friendlyName: String) : CommandElement(key, true) {

    override fun parseValue(arg: String): Any {
        throw IllegalAccessError("This shouldn't be called!")
    }

    fun customParse(args: List<String>): String {
        return args.joinToString(" ")
    }
}