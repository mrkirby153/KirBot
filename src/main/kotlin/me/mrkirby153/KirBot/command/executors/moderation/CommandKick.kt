package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.success
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message

@Command(name = "kick", clearance = Clearance.BOT_MANAGER, description = "Kicks the given user",
        requiredPermissions = arrayOf(Permission.KICK_MEMBERS))
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
        server.kick(user.id)
        message.send().success("Kicked ${user.name}!").queue()
    }
}