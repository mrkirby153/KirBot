package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.ArgumentList
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.CommandElement
import net.dv8tion.jda.core.entities.User
import java.util.regex.Pattern

class UserElement : CommandElement<User> {
    override fun parse(list: ArgumentList): User {
        val first = list.popFirst()
        if (first.matches(Regex("<@!?\\d+>"))) {
            Bot.LOG.debug("Extracting user via mention")
            // Extract out only the numbers
            val pattern = Pattern.compile("\\d+")
            val matcher = pattern.matcher(first)
            if (matcher.find()) {
                val id = first.substring(matcher.start() until matcher.end())

                return Bot.shardManager.getUser(id) ?: throw ArgumentParseException("The user $first was not found!")
            }
        } else if (first.matches(Regex("\\d+"))) {
            Bot.LOG.debug("Extracting user via id")
            // Check their ID
            val pattern = Pattern.compile("\\d+")
            val matcher = pattern.matcher(first)
            if (matcher.find()) {
                val id = first.substring(matcher.start() until matcher.end())

                return Bot.shardManager.getUser(id) ?: throw ArgumentParseException("The user $first was not found!")
            }
        }
        throw ArgumentParseException("The user $first was not found")
    }
}