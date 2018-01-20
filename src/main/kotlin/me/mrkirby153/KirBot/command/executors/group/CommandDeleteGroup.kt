package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.group.Group
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command("deleteGroup,dg")
@RequiresClearance(Clearance.SERVER_ADMINISTRATOR)
class CommandDeleteGroup : BaseCommand(CommandCategory.GROUPS, Arguments.string("name")) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException(
                "Please specify a group name")

        val groups = Model.get(Group::class.java, Pair("server_id", context.guild.id))

        val group = groups.firstOrNull { it.name.equals(name, true) } ?: throw CommandException(
                "A group with that name was not found!")
        group.delete()

        context.send().success("Deleted group `$name`").queue()
    }
}