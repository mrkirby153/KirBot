package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.entities.User

class CommandKick : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user to kick!")
        context.guild.controller.kick(user.id).queue()
        context.send().success("Kicked ${user.name}!").queue()
    }
}