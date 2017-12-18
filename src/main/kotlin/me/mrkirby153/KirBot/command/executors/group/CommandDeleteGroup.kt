package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.*
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command("deleteGroup,dg")
@RequiresClearance(Clearance.SERVER_ADMINISTRATOR)
class CommandDeleteGroup : BaseCommand(CommandCategory.GROUPS, Arguments.string("name")) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException("Please specify a group name")

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