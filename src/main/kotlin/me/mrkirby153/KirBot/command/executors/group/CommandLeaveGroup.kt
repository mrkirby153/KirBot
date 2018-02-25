package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.group.Group
import me.mrkirby153.KirBot.database.models.group.GroupMember
import me.mrkirby153.KirBot.utils.Context

@Command(name = "leaveGroup,lg", arguments = ["<name:string...>"])
class CommandLeaveGroup : BaseCommand(CommandCategory.GROUPS) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException("Please specify a name")

        val group = Model.get(Group::class.java,
                Pair("server_id", context.guild.id)).firstOrNull {
            it.name.equals(name, true)
        } ?: throw CommandException("That group doesn't exist")

        val member = Model.first(GroupMember::class.java,
                Pair("group_id", group.id)) ?: throw CommandException(
                "You are not a member of that group")

        member.delete()

        context.guild.controller.removeRolesFromMember(context.member, group.role).queue()
    }
}