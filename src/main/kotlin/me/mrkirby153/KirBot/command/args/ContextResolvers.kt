package me.mrkirby153.KirBot.command.args

import me.mrkirby153.KirBot.Bot
import java.util.regex.Pattern

object ContextResolvers {

    private val resolvers = mutableMapOf<String, (ArgumentList) -> Any?>()

    init {
        registerDefaultResolvers()
    }

    fun registerResolver(name: String, function: (ArgumentList) -> Any?) {
        Bot.LOG.debug("Registered resolver ")
        resolvers[name] = function
    }


    fun getResolver(name: String): ((ArgumentList) -> Any?)? {
        return resolvers[name]
    }


    private fun registerDefaultResolvers() {
        // String resolver
        // TODO 2/24/18: Make "string..." as the thing that takes the rest
        registerResolver("string") { args ->
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
        registerResolver("string...") { args ->
            return@registerResolver buildString {
                while(args.peek() != null)
                    append(args.popFirst()+" ")
            }.trim().replace(Regex("^(?<!\\\\)\\\""), "").replace(Regex("(?<!\\\\)\\\"\$"), "")
        }

        // Snowflake resolver
        registerResolver("snowflake") { args ->
            val first = args.popFirst()
            val pattern = Pattern.compile("\\d{17,18}")
            val matcher = pattern.matcher(first)
            if(matcher.find()) {
                try{
                    return@registerResolver matcher.group()
                } catch (e: IllegalStateException) {
                    throw ArgumentParseException("`$first` is not a valid snowflake")
                }
            } else {
                throw ArgumentParseException("`$first` is not a valid snowflake")
            }
        }

        // User resolver
        registerResolver("user") { args ->
            val id = getResolver("snowflake")?.invoke(args) as? String
                    ?: throw ArgumentParseException(
                            "Could not find user")

            if (id.toLongOrNull() == null)
                throw ArgumentParseException("Cannot convert `$id` to user")

            return@registerResolver Bot.shardManager.getUserById(id)
                    ?: throw ArgumentParseException(
                            "The user `$id` was not found")
        }

        // Number resolver
        registerResolver("number") { args ->
            val num = args.popFirst()
            try {
                val number = num.toDouble()

                return@registerResolver number
            } catch (e: NumberFormatException) {
                throw ArgumentParseException("`$num` is not a number!")
            }
        }

        // Int resolver
        registerResolver("int") { args ->
            (getResolver("number")?.invoke(args) as? Double)?.toInt()
                    ?: throw ArgumentParseException("Could not parse")
        }
    }
}