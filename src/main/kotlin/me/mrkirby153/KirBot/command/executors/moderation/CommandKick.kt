package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.Permission
import java.awt.Color

@Command(name = "kick", clearance = Clearance.BOT_MANAGER, description = "Kicks the given user",
        requiredPermissions = arrayOf(Permission.KICK_MEMBERS), category = "Moderation")
class CommandKick : CommandExecutor() {
    override fun execute(context: Context, args: Array<String>) {
        if(args.isEmpty()){
            context.send().error("Please mention a user to kick").queue()
            return
        }
        val user = getUserByMention(args[0])
        if (user == null) {
            context.send().error("I could not find a user by that name!").queue()
            return
        }
        guild.controller.kick(user.id).queue()
        context.send().success("Kicked ${user.name}!").queue()
        serverData.logger.log("Kick", "${user.name} has been **kicked** by ${context.author.name}", Color.GREEN)
    }
}