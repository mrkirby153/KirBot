package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.hide
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel

@Command(name = "hideChannel", clearance = Clearance.BOT_MANAGER, deleteCallingMessage = true, description = "Hides this channel from everyone",
        category = "Moderation", requiredPermissions = arrayOf(Permission.MANAGE_CHANNEL))
class CommandHideChannel : CommandExecutor(){

    override fun execute(context: Context, args: Array<String>) {
        // Hide the channel from everyone except the caller and admins
        val channel = (context.channel as? TextChannel) ?: return
        channel.hide()
        context.send().success("Channel hidden!").complete()
    }
}