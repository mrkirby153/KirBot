package me.mrkirby153.KirBot.command.executors.group

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.group.Group
import me.mrkirby153.KirBot.utils.Context

@Command("groups")
class CommandListGroups : BaseCommand(CommandCategory.GROUPS) {
    override fun execute(context: Context, cmdContext: CommandContext) {

        val groups = Model.get(Group::class.java, Pair("server_id", context.guild.id))

        val groupString = buildString {
            append("```")
            appendln("Available Groups (${groups.size})")
            appendln("-----------")
            groups.forEach {
                appendln("${it.name} (${it.members.size} members)")
            }
            append("```\n\n")
            append("Use `${cmdPrefix}joinGroup <name>` to join a group!")
        }
        context.channel.sendMessage(groupString).queue()
    }
}