package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

@Command("charinfo", ["<text:string,rest>"])
class CommandCharInfo : BaseCommand(false) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val text = cmdContext.get<String>("text") ?: return

        context.channel.sendMessage(buildString {
            appendln("```fix")
            text.codePoints().forEach { point ->
                appendCodePoint(point)
                append(" - ")
                append("(" + String.format("U+%04X", point) + ") ")
                append(Character.getName(point))
                append("\n")
            }
            appendln("```")
        }).queue()
    }
}