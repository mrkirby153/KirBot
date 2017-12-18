package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.command.args.ArgumentList
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.CommandElement

class NumberElement(private val min: Double, private val max: Double) : CommandElement<Double> {

    override fun parse(list: ArgumentList): Double {
        val num = list.popFirst()
        try {
            val number = num.toDouble()

            if (number < min) {
                throw ArgumentParseException(String.format("The number you specified (%.2f) must be greater than %.1f", number, min))
            }
            if (number > max) {
                throw ArgumentParseException(String.format("The number you specified (%.2f) must be less than %.1f", number, max))
            }
            return number
        } catch (e: NumberFormatException) {
            throw ArgumentParseException("$num is not a number!")
        }
    }
}