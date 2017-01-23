package me.mrkirby153.KirBot.command.executors.server

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "authorize", clearance = Clearance.SERVER_ADMINISTRATOR, description = "Authorize the client to perform actions on the server")
class CommandAuthorize : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        if(args.size < 2){
            note.error("Missing arguments: <id> <secret>")
            return
        }
        val data = server.data()
        data.authorizeServer(args[0], args[1])
        data.save()
        note.success("Authorized server ${args[0]}")
    }
}
