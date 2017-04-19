package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color

@Command(name = "updatenames", clearance = Clearance.SERVER_ADMINISTRATOR)
class UpdateNicknames : CommandExecutor() {
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        note.delete().queue()
        val realnameHandler = RealnameHandler(server)
        realnameHandler.updateNames()
        note.replyEmbed("Success", "Real names were refreshed from the database", Color.GREEN)
    }
}