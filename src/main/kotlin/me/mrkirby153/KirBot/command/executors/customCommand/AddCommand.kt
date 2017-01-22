package me.mrkirby153.KirBot.command.executors.customCommand

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.server.data.CommandType
import me.mrkirby153.KirBot.server.data.CustomServerCommand
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "addCmd", clearance = Clearance.SERVER_ADMINISTRATOR, description = "Adds a command to the server")
class AddCommand : CommandExecutor() {

    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        // !addCmd <name> <Type> <Command>
        if (args.size < 3) {
            note.error("Invalid arguments\nArguments: `<name> <type> <command>`")
            return
        }
        val name = args[0]
        val type = CommandType.valueOf(args[1].toUpperCase())
        val command = args.drop(2).joinToString(" ")

        val serverData = server.data()
        serverData.commands[name.toLowerCase()] = CustomServerCommand(type, Clearance.USER, command)
        serverData.save()
        note.info("Added command `$name`")
    }
}