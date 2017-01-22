package me.mrkirby153.KirBot.command.executors.server

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import java.util.regex.Pattern

@Command(name = "cmdPrefix", clearance = Clearance.SERVER_OWNER, description = "Sets the prefix to invoke commands")
class SetCommandPrefix : CommandExecutor() {
    private val PREFIX_REGEX = "[A-Za-z0-9]"
    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        if (args.isEmpty()) {
            note.error("Missing argument: `<prefix>`")
            return
        }
        val prefix = args[0]
        if (prefix.length > 1) {
            note.error("Prefixes can only be one character")
            return
        }
        val matcher = Pattern.compile(PREFIX_REGEX).matcher(prefix)
        if (matcher.find()) {
            note.error("Prefixes cannot be letters or numbers!")
            return
        }
        val data = server.data()
        data.commandPrefix = args[0]
        data.save()
        note.success("Updated command prefix to `${data.commandPrefix}`")
    }
}