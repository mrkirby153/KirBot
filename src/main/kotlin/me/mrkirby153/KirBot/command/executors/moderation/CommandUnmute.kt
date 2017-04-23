package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color

@Command(name = "unmute", description = "Unmutes a user", clearance = Clearance.BOT_MANAGER, requiredPermissions = arrayOf(Permission.MANAGE_CHANNEL))
class CommandUnmute : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        if (args.isEmpty()) {
            note.error("Please mention a user to mute")
            return
        }

        val user = server.getMember(getUserByMention(args[0]))

        if (user == null) {
            note.error("Could not find a user by that name!")
            return
        }

        if (channel !is TextChannel) {
            note.error("This command doesn't work in PMs :(")
            return
        }

        val override = channel.getPermissionOverride(user)
        if (override == null || !override.denied.contains(Permission.MESSAGE_WRITE)) {
            note.replyEmbed(null, "That user isn't muted", Color.YELLOW)
            return
        }
        if (override.denied.size > 1) {
            if (override.denied.contains(Permission.MESSAGE_WRITE)) {
                override.manager.clear(Permission.MESSAGE_WRITE).queue()
            }
        } else {
            override.delete().queue()
        }
        note.success("${user.effectiveName} has been unmuted!")
    }
}