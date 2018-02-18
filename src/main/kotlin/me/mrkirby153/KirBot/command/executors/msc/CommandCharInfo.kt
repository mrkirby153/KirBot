package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

@Command("charinfo")
class CommandCharInfo : BaseCommand(false, arguments = *arrayOf(Arguments.restAsString("text"))) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val text = cmdContext.get<String>("text") ?: return

        context.channel.sendMessage(buildString {
            appendln("```fix")
            text.codePoints().forEach { point ->
                appendCodePoint(point)
                append(" - ")
                append(Character.getName(point))
                append("\n")
            }
            appendln("```")
        }).queue()
    }
}