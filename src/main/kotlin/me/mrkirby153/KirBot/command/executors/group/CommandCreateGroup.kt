package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.Group
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context

class CommandCreateGroup : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val name = cmdContext.string("name") ?: throw CommandException("Please specify a name")

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