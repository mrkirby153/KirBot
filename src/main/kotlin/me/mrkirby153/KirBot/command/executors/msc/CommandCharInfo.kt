package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context


class CommandCharInfo {

    @Command("charinfo", arguments = ["<text:string...>"], category = CommandCategory.MISCELLANEOUS)
    @CommandDescription("Get information about a string of characters")
    fun execute(context: Context, cmdContext: CommandContext) {
        val text = cmdContext.get<String>("text") ?: return

        val string = buildString {
            appendln("```fix")
            text.codePoints().forEach { point ->
                appendCodePoint(point)
                append(" - ")
                append("(" + String.format("U+%04X", point) + ") ")
                append(Character.getName(point))
                append("\n")
            }
            appendln("```")
        }
        if (string.length > 2000) {
            context.channel.sendFile(string.toByteArray(), "charinfo.txt").queue()
        } else {
            context.channel.sendMessage(string).queue()
        }
    }
}