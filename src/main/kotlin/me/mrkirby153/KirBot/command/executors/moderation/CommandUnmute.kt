package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import java.awt.Color

@Command(name = "unmute", description = "Unmutes a user", clearance = Clearance.BOT_MANAGER,
        requiredPermissions = arrayOf(Permission.MANAGE_CHANNEL), category = "Moderation")
class CommandUnmute : CommandExecutor() {
    override fun execute(context: Context, args: Array<String>) {
        if (args.isEmpty()) {
            context.send().error("Please mention a user to mute!").queue()
            return
        }

        val user = guild.getMember(getUserByMention(args[0]))

        if (user == null) {
            context.send().error("Could not find a user by that name!").queue()
            return
        }

        if (context.channel !is TextChannel) {
            context.send().error("This command doesn't work in PMs :(").queue()
            return
        }

        val override = (context.channel as TextChannel).getPermissionOverride(user)

        if (override == null || !override.denied.contains(Permission.MESSAGE_WRITE)) {
            context.send().embed("Warning") {
                setColor(Color.YELLOW)
                setDescription("That user isn't muted!")
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
        context.send().success("User unmuted!").queue()
        serverData.logger.log("Mute", "${user.effectiveName} has been **unmuted** by ${context.author.name}")
    }
}