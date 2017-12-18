package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context

@Command("groups")
class CommandListGroups : BaseCommand(CommandCategory.GROUPS){
    override fun execute(context: Context, cmdContext: CommandContext) {
        PanelAPI.getGroups(context.guild).queue {
            val groups = buildString {
                append("```")
                appendln("Avalilable Groups (${it.size})")
                appendln("-----------")
                it.forEach {
                    appendln("${it.name} (${it.members.size} members)")
                }
                append("```\n\n")
                append("Use `!joinGroup <name>` to join a group!")
            }
            context.channel.sendMessage(groups).queue()
        }
    }
}