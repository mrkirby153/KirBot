package me.mrkirby153.KirBot.command.executors.customCommand

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "delCmd", clearance = Clearance.SERVER_ADMINISTRATOR, description = "Removes a command from the server")
class DeleteCommand : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        if (args.isEmpty()) {
            note.error("Missing arguments!\nArguments: <name>")
            return
        }

        val key = args[0].toLowerCase()
        val data = server.data()
        data.commands.remove(key)
        data.save()

        note.info("Deleted command `" + args[0] + "`")
    }
}