package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.*
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.Group
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command("createGroup,cg")
@RequiresClearance(Clearance.SERVER_ADMINISTRATOR)
class CommandCreateGroup : BaseCommand(CommandCategory.GROUPS, Arguments.string("name")){

    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException("Please specify a name")

        PanelAPI.getGroups(context.guild).queue { groups ->
            if (name.toLowerCase() in groups.map { it.name.toLowerCase() }) {
                context.send().error("A group with that name already exists!").queue()
                return@queue
            }
            context.guild.controller.createRole().setName(name).setMentionable(true).queue {
                Group.create(context.guild, name, it).queue {
                    context.send().info("Created the group `$name`").queue()
                }
            }
        }
    }
}