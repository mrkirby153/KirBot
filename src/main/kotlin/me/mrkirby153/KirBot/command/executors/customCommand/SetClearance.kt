package me.mrkirby153.KirBot.command.executors.customCommand

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "cmdClearance", clearance = Clearance.SERVER_ADMINISTRATOR, description = "Set the clearance required to run a command")
class SetClearance : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        if (args.size < 2) {
            note.error("Invalid arguments\nArguments: <name> <clearance>")
            return
        }
        val cmdName = args[0]
        val clerance = Clearance.valueOf(args[1].toUpperCase())

        if (sender.getClearance(server).value < clearance.value) {
            note.error("Cannot set clearance to higher than your own!")
            return
        }

        val data = server.data()
        if (data.commands[cmdName.toLowerCase()] == null) {
            note.error("That command does not exist!")
            return
        }
        data.commands[cmdName.toLowerCase()]?.clearance = clearance
        data.save()
        note.info("Set command clearance to $clerance")
    }
}