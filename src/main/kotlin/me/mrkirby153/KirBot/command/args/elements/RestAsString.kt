package me.mrkirby153.KirBot.command.args.elements

import me.mrkirby153.KirBot.command.args.ArgumentList
import me.mrkirby153.KirBot.command.args.CommandElement

class RestAsString : CommandElement<String> {
    override fun parse(list: ArgumentList) = buildString {
        while (list.peek() != null)
            append(list.popFirst() + " ")
    }.trim().replace(Regex("^(?<!\\\\)\\\""), "").replace(Regex("(?<!\\\\)\\\"\$"), "")
}