package me.mrkirby153.KirBot.command.executors.server

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "password", clearance = Clearance.SERVER_ADMINISTRATOR)
class CommandPassword : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        val data = server.data()
        if (args.size == 1) {
            if (args[0] == "reset") {
                data.regeneratePassword()
                data.save()
                note.info("Password has been reset!").get().delete(10)
            }
        }
        note.info("Server Password\n" + "`" + data.serverPassword + "`\nThis message will be deleted in 10 seconds").get().delete(10)
    }
}