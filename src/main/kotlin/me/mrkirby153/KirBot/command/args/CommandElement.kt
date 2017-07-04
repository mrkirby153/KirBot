package me.mrkirby153.KirBot.command.args

import me.mrkirby153.KirBot.command.args.elements.*

abstract class CommandElement(val key: String, val required: Boolean = true) {

    abstract val friendlyName: String


    fun parse(arg: String, context: CommandContext) {
        val returned = this.parseValue(arg)
        context.put(key, returned)
    }


    abstract fun parseValue(arg: String): Any
}

class ArgumentParseException(msg: String) : Exception(msg)

object Arguments {

    val URL_REGEX = "(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,4}\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)"
    @JvmOverloads
    fun number(key: String, required: Boolean = true, min: Double = Double.MIN_VALUE, max: Double = Double.MAX_VALUE ): CommandElement {
        return NumberElement(key, required, min, max)
    }

    fun string(key: String, required: Boolean = true, name: String = "String"): CommandElement {
        return StringElement(key, required, name)
    }

    fun user(key: String, required: Boolean = true): CommandElement {
        return UserElement(key, required)
    }

    fun rest(key: String, name: String = "message"): CommandElement {
        return RestToString(key, name)
    }

    fun regex(key: String, regex: String, required: Boolean = true, name: String = "Regex", error: String? = null): CommandElement{
        return RegexElemenet(key, required, regex, name, error)
    }

    fun url(key: String, required: Boolean = true): CommandElement {
        return regex(key, URL_REGEX, required, "URL")
    }
}