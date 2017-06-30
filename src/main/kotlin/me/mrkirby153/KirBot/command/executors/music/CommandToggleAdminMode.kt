package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import java.awt.Color.PINK

@Command(name = "musicadmin", description = "Toggles admin-only control of the DJ", clearance = Clearance.SERVER_ADMINISTRATOR, category = "Music")
class CommandToggleAdminMode : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val musicManager = context.data.musicManager
        var shouldHalt = false
        cmdContext.has<String>("action") { action ->
            if (action.equals("status", true)) {
                adminStatus(context, musicManager)
            }
            shouldHalt = true
        }
        if(shouldHalt)
            return
        musicManager.adminOnly = !musicManager.adminOnly

        adminStatus(context, musicManager)
    }

    private fun adminStatus(context: Context, musicManager: MusicManager) {
        context.send().embed("Admin Music Mode") {
            setColor(PINK)
            setDescription(if (musicManager.adminOnly) "ON" else "OFF")
        }.rest().queue()
    }
}