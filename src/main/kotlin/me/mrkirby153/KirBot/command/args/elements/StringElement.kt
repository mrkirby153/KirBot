package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.command.args.CommandElement

class StringElement(key: String, required: Boolean, override val friendlyName: String) : CommandElement(key, required) {


    override fun parseValue(arg: String): Any {
        return arg
    }
}