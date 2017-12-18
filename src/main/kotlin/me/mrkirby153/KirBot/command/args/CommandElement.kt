package me.mrkirby153.KirBot.command.args

import me.mrkirby153.KirBot.command.args.elements.*


interface CommandElement<out T> {
    fun parse(list: ArgumentList): T
}


data class Argument(val key: String, val element: CommandElement<*>, val required: Boolean)

class ArgumentParseException(msg: String) : Exception(msg)

object Arguments {
    val URL_REGEX = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)"

    fun string(key: String, required: Boolean = true)= Argument(key, StringElement(), required)

    fun regex(key: String, regex: String, required: Boolean = true, error: String? = null) = Argument(key, RegexElement(regex, error), required)

    fun url(key: String, required: Boolean = true) = Argument(key, RegexElement(URL_REGEX, "Provide a valid URL"), required)

    fun restAsString(key: String) = Argument(key, RestAsString(), false)

    fun number(key: String, required: Boolean = true, min : Number = Double.MIN_VALUE, max: Number = Double.MAX_VALUE) = Argument(key, NumberElement(min.toDouble(), max.toDouble()), required)

    fun user(key: String, required : Boolean = true) = Argument(key, UserElement(), required)
}