package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.group.Group
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.utils.IdGenerator

@Command(name = "createGroup,cg", arguments = ["<name:string...>"],
        clearance = Clearance.SERVER_ADMINISTRATOR)
class CommandCreateGroup : BaseCommand(CommandCategory.GROUPS) {

    override fun execute(context: Context, cmdContext: CommandContext) {
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
}