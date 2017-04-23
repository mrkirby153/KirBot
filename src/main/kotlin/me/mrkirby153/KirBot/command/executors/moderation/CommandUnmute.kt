package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.success
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.awt.Color

@Command(name = "unmute", description = "Unmutes a user", clearance = Clearance.BOT_MANAGER, requiredPermissions = arrayOf(Permission.MANAGE_CHANNEL))
class CommandUnmute : CommandExecutor() {
    override fun execute(message: Message, args: Array<String>) {
        if (args.isEmpty()) {
            message.send().error("Please mention a user to mute!").queue()
            return
        }

        val user = server.getMember(getUserByMention(args[0]))

        if (user == null) {
            message.send().error("Could not find a user by that name!").queue()
            return
        }

        if (message.channel !is TextChannel) {
            message.send().error("This command doesn't work in PMs :(").queue()
            return
        }

        val override = (message.channel as TextChannel).getPermissionOverride(user)

        if (override == null || !override.denied.contains(Permission.MESSAGE_WRITE)) {
            message.send().embed {
                color = Color.YELLOW
                title = "Warning"
                description = "That user isn't muted!"
            }.rest().queue()
            return
        }
        if (override.denied.size > 1) {
            if (override.denied.contains(Permission.MESSAGE_WRITE)) {
                override.manager.clear(Permission.MESSAGE_WRITE).queue()
            }
        } else {
            override.delete().queue()
        }
        message.send().success("User unmuted!").queue()
    }
}