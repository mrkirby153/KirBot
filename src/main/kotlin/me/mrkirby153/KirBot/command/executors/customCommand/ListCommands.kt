package me.mrkirby153.KirBot.command.executors.customCommand

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "cmds", aliases = arrayOf("commands"), description = "Lists all commands available on the server")
class ListCommands : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        note.info("Available commands: `" + server.data().commands.keys.joinToString() + "`")
    }

}