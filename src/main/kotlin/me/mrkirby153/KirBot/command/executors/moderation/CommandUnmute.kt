package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color

class CommandUnmute : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user to unmute!")
        val member = user.getMember(context.guild) ?: throw CommandException("The user is not a member of this guild!")

        if(context.channel !is TextChannel){
            throw CommandException("This command doesn't work in PMs")
        }

        val override = context.channel.getPermissionOverride(member)
        if(override == null || !override.denied.contains(Permission.MESSAGE_WRITE)){
            context.send().embed("Warning"){
                setColor(Color.YELLOW)
                setDescription("That user isn't muted!")
            }.rest().queue()
            return
        }
        if(override.denied.size > 1){
            if(override.denied.contains(Permission.MESSAGE_WRITE))
                override.manager.clear(Permission.MESSAGE_WRITE).queue()
        } else {
            override.delete().queue()
        }
        context.send().success("${member.effectiveName} has been unmuted!").queue()
    }
}