package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User

class CommandMute : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user to mute")

        val member = user.getMember(context.guild) ?: throw CommandException("This user isn't a part of the guild!")
        if(context.channel !is TextChannel)
            throw CommandException("This command won't work in PMs")

        val channel = context.channel
        val override = channel.getPermissionOverride(member) ?: channel.createPermissionOverride(member).complete()
        override.manager.deny(Permission.MESSAGE_WRITE).queue()
        context.send().success("${member.effectiveName} has been muted!").queue()
    }
}