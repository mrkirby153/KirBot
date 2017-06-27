package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.TextChannel

@Command(name = "hideChannel", clearance = Clearance.BOT_MANAGER, deleteCallingMessage = true, description = "Hides this channel from everyone",
        category = "Moderation", requiredPermissions = arrayOf(Permission.MANAGE_CHANNEL))
class CommandHideChannel : CommandExecutor(){

    override fun execute(context: Context, args: Array<String>) {
        // Hide the channel from everyone except the caller and admins
        val channel = (context.channel as? TextChannel) ?: return
        // Check overrides & remove message read from them
        channel.permissionOverrides.filter { it.allowed.contains(Permission.MESSAGE_READ) }.forEach {
            it.manager.clear(Permission.MESSAGE_READ).queue()
        }
        val role = channel.getPermissionOverride(context.guild.publicRole) ?: channel.createPermissionOverride(context.guild.publicRole).complete()
        role.manager.deny(Permission.MESSAGE_READ).complete()

        // Check if user has access
        if(!context.member.hasPermission(context.channel as Channel, Permission.MESSAGE_READ)){
            val userOverride = channel.getPermissionOverride(context.member) ?: channel.createPermissionOverride(context.member).complete()
            userOverride.manager.grant(Permission.MESSAGE_READ).complete()
        }
        // Give the robot access
        if(!context.member.hasPermission(context.channel, Permission.MESSAGE_READ)){
            val botOverride = channel.getPermissionOverride(context.guild.selfMember) ?: channel.createPermissionOverride(context.guild.selfMember).complete()
            botOverride.manager.grant(Permission.MESSAGE_READ).complete()
        }
        context.send().success("Channel hidden!").complete()
    }
}