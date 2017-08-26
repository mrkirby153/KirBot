package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context

class CommandDeleteGroup : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.string("name") ?: throw CommandException("Please specify a name")

        PanelAPI.getGroups(context.guild).queue {
            it.forEach {
                if (it.name.toLowerCase() == name.toLowerCase()) {
                    context.guild.getRoleById(it.roleId)?.delete()?.queue()
                    it.delete().queue {
                        context.send().success("Deleted group `$name`").queue()
                    }
                }
            }
        }
    }
}