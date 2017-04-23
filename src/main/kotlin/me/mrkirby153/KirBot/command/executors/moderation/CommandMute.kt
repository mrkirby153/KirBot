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

@Command(name = "mute", description = "Mute a given user", clearance = Clearance.BOT_MANAGER, requiredPermissions = arrayOf(Permission.MANAGE_CHANNEL))
class CommandMute : CommandExecutor() {
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

        var override = channel.getPermissionOverride(user)
        if (override == null) {
            override = channel.createPermissionOverride(user).complete(true)
        }
        override.manager.deny(Permission.MESSAGE_WRITE).queue()
        note.success("${user.effectiveName} has been muted!")
    }
}