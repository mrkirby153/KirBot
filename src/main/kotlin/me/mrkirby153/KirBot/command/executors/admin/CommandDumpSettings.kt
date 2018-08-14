package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context

@Command(name = "dumpSettings", clearance = CLEARANCE_ADMIN)
class CommandDumpSettings : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val settings = context.kirbotGuild.settings

        context.channel.sendMessage(buildString {
            append("General Settings\n")
            appendln("```")
            settings.columnData.forEach { k, v ->
                append(k)
                append(" = ")
                append(v?.toString() ?: "Null")
                append("\n")
            }
            append("```")
        }).queue()

        val musicSettings = context.kirbotGuild.musicManager.settings
        context.channel.sendMessage(buildString {
            append("Music Settings\n")
            appendln("```")
            musicSettings.columnData.forEach { k, v ->
                append(k)
                append(" = ")
                append(v.toString())
                append("\n")
            }
            append("```")
        }).queue()
    }
}