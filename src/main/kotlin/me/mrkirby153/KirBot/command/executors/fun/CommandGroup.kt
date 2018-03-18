package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.group.Group
import me.mrkirby153.KirBot.database.models.group.GroupMember
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.utils.IdGenerator

@Command(name = "group,groups")
class CommandGroup : BaseCommand(CommandCategory.GROUPS) {

    // Default command, show the available groups
    override fun execute(context: Context, cmdContext: CommandContext) {
        val groups = Model.get(Group::class.java, Pair("server_id", context.guild.id))

        val groupString = buildString {
            append("```")
            val groupHeader = "Available Groups (${groups.size}):"
            appendln(groupHeader)
            appendln("-".repeat(groupHeader.length))
            groups.forEach {
                appendln("${it.name} (${it.members.size} members)")
            }
            append("```\n")
            append("Use `${cmdPrefix}group join <name>` to join a group")
        }
        context.channel.sendMessage(groupString).queue()
    }

    @Command(name = "join", arguments = ["<group:string...>"])
    fun joinGroup(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("group") ?: throw CommandException(
                "Please specify a group to join")

        val groups = Model.get(Group::class.java, Pair("server_id", context.guild.id))

        val group = groups.firstOrNull { it.name.equals(name, true) } ?: throw CommandException(
                "That group doesn't exist!")

        val member = Model.first(GroupMember::class.java, Pair("user_id", context.author.name),
                Pair("group_id", group.id))
        if (member != null)
            throw CommandException("You are already a member of this group")

        val newMember = GroupMember()
        newMember.id = Model.randomId()
        newMember.user = context.author
        newMember.groupId = group.id
        newMember.save()
        context.guild.controller.addRolesToMember(context.member, group.role).queue {
            context.send().success("You have joined `$name`").queue()
        }
    }

    @Command(name = "leave", arguments = ["<group:string...>"])
    fun leaveGroup(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("group") ?: throw CommandException(
                "Please specify a group")

        val group = Model.get(Group::class.java,
                Pair("server_id", context.guild.id)).firstOrNull {
            it.name.equals(name, true)
        } ?: throw CommandException("That group doesn't exist")

        val member = Model.first(GroupMember::class.java,
                Pair("group_id", group.id)) ?: throw CommandException(
                "You are not a member of that group")

        member.delete()

        context.guild.controller.removeRolesFromMember(context.member, group.role).queue()
        context.send().success("You left group `$name`").queue()
    }

    @Command(name = "create", clearance = CLEARANCE_ADMIN, arguments = ["<name:string...>"])
    fun createGroup(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException("Please specify a name")

        val groups = Model.get(Group::class.java, Pair("server_id", context.guild.id))
        if (name in groups.map { it.name.toLowerCase() }) {
            throw CommandException("A group with that name already exists!")
        }
        val group = Group()
        group.id = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS).generate()
        group.name = name
        group.server = context.guild
        group.save()
        context.guild.controller.createRole().queue { role ->
            group.role = role
            group.save()
            val manager = role.managerUpdatable
            manager.mentionableField.value = true
            manager.nameField.value = name
            manager.update().queue()
        }
        context.send().success("Created group `$name`").queue()
    }

    @Command(name = "delete", clearance = CLEARANCE_ADMIN, arguments = ["<name:string...>"])
    fun deleteGroup(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.get<String>("name") ?: throw CommandException(
                "Please specify a group name")

        val groups = Model.get(Group::class.java, Pair("server_id", context.guild.id))

        val group = groups.firstOrNull { it.name.equals(name, true) } ?: throw CommandException(
                "A group with that name was not found!")
        group.role?.delete()?.queue()
        group.delete()

        context.send().success("Deleted group `$name`").queue()
    }
}