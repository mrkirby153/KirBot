package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.success
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel

@Command(name = "mute", description = "Mute a given user", clearance = Clearance.BOT_MANAGER,
        requiredPermissions = arrayOf(Permission.MANAGE_CHANNEL), category = "Moderation")
class CommandMute : CommandExecutor() {
    override fun execute(message: Message, args: Array<String>) {
        if (args.isEmpty()) {
            message.send().error("Please mention a user to mute").queue()
            return
        }

        val user = guild.getMember(getUserByMention(args[0]))

        if (user == null) {
            message.send().error("Could not find a user by that name!").queue()
            return
        }

        if (message.channel !is TextChannel) {
            message.send().error("This command doesn't work in PMs :(").queue()
            return
        }

        val channel = message.channel as TextChannel
        var override = channel.getPermissionOverride(user)
        if (override == null) {
            override = channel.createPermissionOverride(user).complete(true)
        }
        override.manager.deny(Permission.MESSAGE_WRITE).queue()
        message.send().success("${user.effectiveName} has been muted!").queue()
        serverData.logger.log("Mute", "${user.effectiveName} has been **muted** by ${message.author.name}")
    }
}