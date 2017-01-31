package me.mrkirby153.KirBot.command.executors.server

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "bridgeSet", clearance = Clearance.SERVER_ADMINISTRATOR)
class SetBridge : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        val data = server.data()
        data.bridgeChannel = channel.id
        data.save()
        note.success("Set channel!")
    }
}