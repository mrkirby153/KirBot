package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

@Command("charinfo", ["<text:string...>"])
class CommandCharInfo : BaseCommand(false) {
    override fun execute(context: Context, cmdContext: CommandContext) {
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