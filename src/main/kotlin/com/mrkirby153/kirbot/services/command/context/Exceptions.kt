package com.mrkirby153.kirbot.services.command.context

/**
 * Generic exception thrown when an error occurs when parsing an argument
 */
open class ArgumentParseException(msg: String): Exception(msg)

/**
 * Exception thrown when a resolver is missing
 */
class MissingResolverException(type: Class<*>) : ArgumentParseException("Missing resolver for $type")

class MissingArgumentException(argumentName: String): ArgumentParseException("$argumentName is required")