package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import java.awt.Color.PINK

@Command(name = "musicadmin", description = "Toggles admin-only control of the DJ", clearance = Clearance.SERVER_ADMINISTRATOR, category = "Music")
class CommandToggleAdminMode : CommandExecutor() {
    override fun execute(context: Context, args: Array<String>) {
        if (!args.isEmpty()) {
            if (args[0] == "status") {
                context.send().embed("Admin Music Mode") {
                    setColor(PINK)
                    setDescription(if (serverData.musicManager.adminOnly) "ON" else "OFF")
                }.rest().queue()
                return
            }
        }
        serverData.musicManager.adminOnly = !serverData.musicManager.adminOnly

        context.send().embed("Admin Music Mode") {
            setColor(PINK)
            setDescription(if (serverData.musicManager.adminOnly) "ON" else "OFF")
        }.rest().queue()
    }
}