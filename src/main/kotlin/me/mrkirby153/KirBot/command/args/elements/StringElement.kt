package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.ArgumentList
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.CommandElement

class StringElement : CommandElement<String> {

    override fun parse(list: ArgumentList): String {
        return if (list.peek().matches(Regex("^(?<!\\\\)\\\".*"))) {
            Bot.LOG.debug("Found beginning quote, starting parse")
            val string = buildString {
                while (true) {
                    if(!list.hasNext()){
                        throw ArgumentParseException("Unmatched quotes")
                    }
                    val next = list.popFirst()
                    append(next + " ")
                    if (next.matches(Regex(".*(?<!\\\\)\\\"\$"))) {
                        break
                    }
                }
            }
            Bot.LOG.debug("Parse complete!")
            string.trim().substring(1..(string.length-3)).replace("\\\"", "\"")
        } else {
            list.popFirst()
        }
    }
}