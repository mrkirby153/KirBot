package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.CommandElement

class NumberElement(key: String, required: Boolean, val min: Double, val max: Double )
    : CommandElement(key, required) {

    override val friendlyName = "Number"

    override fun parseValue(arg: String): Any {
        try{
            val number = arg.toDouble()
            if(number < min){
                throw ArgumentParseException("The provided number ($number) must be greater than $min")
            }
            if(number > max){
                throw ArgumentParseException("The provided number ($number) must be less than $max")
            }
            return number
        } catch(e: NumberFormatException){
            throw ArgumentParseException("Please enter a number!")
        }
    }
}