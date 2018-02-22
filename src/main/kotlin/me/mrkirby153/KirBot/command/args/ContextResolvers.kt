package me.mrkirby153.KirBot.command.args

import me.mrkirby153.KirBot.Bot
import java.util.regex.Pattern

object ContextResolvers {

    private val resolvers = mutableMapOf<String, (ArgumentList, Array<String>) -> Any?>()

    init {
        registerDefaultResolvers()
    }

    fun registerResolver(name: String, function: (ArgumentList, Array<String>) -> Any?) {
        Bot.LOG.debug("Registered resolver ")
        resolvers[name] = function
    }


    fun getResolver(name: String): ((ArgumentList, Array<String>) -> Any?)? {
        return resolvers[name]
    }


    private fun registerDefaultResolvers() {
        // String resolver
        registerResolver("string") { args, params ->
            if (params.size == 1 && params[0].equals("rest", true)) {
                // Eating the rest of the string
                return@registerResolver buildString {
                    while (args.peek() != null) {
                        append(args.popFirst() + " ")
                    }
                }.trim().replace(Regex("^(?<!\\\\)\\\""), "").replace(Regex("(?<!\\\\)\\\"\$"), "")
            }
            // Return the string in quotes
            if (args.peek().matches(Regex("^(?<!\\\\)\\\".*"))) {
                Bot.LOG.debug("Found beginning quote, starting parse")
                val string = buildString {
                    while (true) {
                        if (!args.hasNext()) {
                            throw ArgumentParseException("Unmatched quotes")
                        }
                        val next = args.popFirst()
                        append(next + " ")
                        if (next.matches(Regex(".*(?<!\\\\)\\\"\$"))) {
                            break
                        }
                    }
                }
                Bot.LOG.debug("Parse complete!")
                string.trim().substring(1..(string.length - 3)).replace("\\\"", "\"")
            } else {
                args.popFirst()
            }
        }

        // Snowflake resolver
        registerResolver("snowflake") { args, _ ->
            val first = args.popFirst()
            if (first.matches(Regex("<@!?\\d+>"))) {
                Bot.LOG.debug("Matching via mention")
                val pattern = Pattern.compile("\\d+")
                val matcher = pattern.matcher(first)
                if (matcher.find()) {
                    try {
                        return@registerResolver matcher.group()
                    } catch (e: IllegalStateException) {
                        throw ArgumentParseException("Failed to extract ID from `$first`")
                    }
                }
            }
            return@registerResolver first
        }

        // User resolver
        registerResolver("user") { args, params ->
            val id = getResolver("snowflake")?.invoke(args, params) as? String
                    ?: throw ArgumentParseException(
                            "Could not find user")

            if (id.toLongOrNull() == null)
                throw ArgumentParseException("Cannot convert `$id` to user")

            return@registerResolver Bot.shardManager.getUser(id)
                    ?: throw ArgumentParseException(
                            "The user `$id` was not found")
        }

        // Number resolver
        registerResolver("number") { args, params ->
            val num = args.popFirst()

            val min = if (params.size == 1) {
                if (params[0] == "x") Double.MIN_VALUE else params[0].toDouble()
            } else Double.MIN_VALUE
            val max = if (params.size == 2) {
                if (params[1] == "x")
                    Double.MAX_VALUE else params[1].toDouble()
            } else Double.MAX_VALUE

            println("Min: $min, Max: $max")

            try {
                val number = num.toDouble()

                if (number < min)
                    throw ArgumentParseException(String.format(
                            "The number you specified (%.2f) must be greater than %.2f", num, min))
                if (number > max)
                    throw ArgumentParseException(
                            String.format("The number you specified (%.2f) must be less than %.2f",
                                    num, max))
                return@registerResolver number
            } catch (e: NumberFormatException) {
                throw ArgumentParseException("$num is not a number!")
            }
        }

        // Int resolver
        registerResolver("int") { args, params ->
            (getResolver("number")?.invoke(args, params) as? Double)?.toInt()
                    ?: throw ArgumentParseException("Could not parse")
        }
    }
}