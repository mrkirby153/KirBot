package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "kick", clearance = Clearance.BOT_MANAGER, description = "Kicks the given user",
        requiredPermissions = arrayOf(Permission.KICK_MEMBERS))
class CommandKick : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        if(args.isEmpty()){
            note.error("Please mention a user to kick")
            return
        }
        val user = getUserByMention(args[0])
        if (user == null) {
            note.error("I could not find a user by that name!")
            return
        }
        server.kick(user.id)
        note.success("Kicked ${user.name}!")
    }
}