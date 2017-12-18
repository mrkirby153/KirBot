package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context

@Command("leaveGroup,lg")
class CommandLeaveGroup : BaseCommand(CommandCategory.GROUPS, Arguments.string("name")){
    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException("Please specify a name")

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