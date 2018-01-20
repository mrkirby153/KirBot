package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.group.Group
import me.mrkirby153.KirBot.database.models.group.GroupMember
import me.mrkirby153.KirBot.utils.Context

@Command("joinGroup,jg")
class CommandJoinGroup : BaseCommand(CommandCategory.GROUPS, Arguments.string("name")) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException("Please specify a name")

        val groups = Model.get(Group::class.java, Pair("server_id", context.guild.id))

        if(name.toLowerCase() !in groups.map { it.name.toLowerCase() })
            throw CommandException("That group does not exist!")

        groups.firstOrNull { it.name.toLowerCase() == name.toLowerCase() }?.run {
            val member = Model.first(GroupMember::class.java, Pair("user_id", context.author.name), Pair("group_id", id))

            if(member != null)
                throw CommandException("You are already a member of this group")

            val newMember = GroupMember()
            newMember.id = Model.randomId()
            newMember.user = context.author
            newMember.groupId = id
            newMember.save()
            context.guild.controller.addRolesToMember(context.member, role).queue()
            context.success()
        }
    }
}