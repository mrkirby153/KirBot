package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Message
import java.awt.Color

@Command(name = "musicadmin", description = "Toggles admin-only control of the DJ", clearance = Clearance.SERVER_ADMINISTRATOR)
class CommandToggleAdminMode : CommandExecutor() {
    override fun execute(message: Message, args: Array<String>) {
        if (!args.isEmpty()) {
            if (args[0] == "status") {
                message.send().embed("Admin Music Mode") {
                    color = Color.PINK
                    description = if (serverData.musicManager.adminOnly) "ON" else "OFF"
                }.rest().queue()
                return
            }
        }
        serverData.musicManager.adminOnly = !serverData.musicManager.adminOnly

        message.send().embed("Admin Music Mode") {
            color = Color.PINK
            description = if (serverData.musicManager.adminOnly) "ON" else "OFF"
        }.rest().queue()
    }
}