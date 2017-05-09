package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.success
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import java.awt.Color

@Command(name = "kick", clearance = Clearance.BOT_MANAGER, description = "Kicks the given user",
        requiredPermissions = arrayOf(Permission.KICK_MEMBERS), category = "Moderation")
class CommandKick : CommandExecutor() {
    override fun execute(message: Message, args: Array<String>) {
        if(args.isEmpty()){
            message.send().error("Please mention a user to kick").queue()
            return
        }
        val user = getUserByMention(args[0])
        if (user == null) {
            message.send().error("I could not find a user by that name!").queue()
            return
        }
        guild.controller.kick(user.id).queue()
        message.send().success("Kicked ${user.name}!").queue()
        serverData.logger.log("Kick", "${user.name} has been **kicked** by ${message.author.name}", Color.GREEN)
    }
}