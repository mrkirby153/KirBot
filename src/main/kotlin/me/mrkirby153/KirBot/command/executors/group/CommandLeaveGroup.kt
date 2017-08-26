package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context

class CommandLeaveGroup : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {

        val name = cmdContext.string("name") ?: throw CommandException("Please specify a name")

        PanelAPI.getGroups(context.guild).queue {
            if (name.toLowerCase() !in it.map { it.name.toLowerCase() }) {
                context.send().error("That group doesn't exist!").queue()
                return@queue
            }
            it.forEach { g ->
                if (g.name.toLowerCase() == name.toLowerCase()) {
                    g.removeUser(context.author)?.queue {
                        context.send().success("You have left `$name`")
                    }
                }
            }
        }
    }
}