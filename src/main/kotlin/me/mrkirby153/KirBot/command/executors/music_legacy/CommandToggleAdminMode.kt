package me.mrkirby153.KirBot.command.executors.music_legacy

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.music_legacy.MusicManager
import me.mrkirby153.KirBot.utils.Context
import java.awt.Color.PINK

class CommandToggleAdminMode : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val musicManager = context.data.musicManager_old
        var shouldHalt = false
        cmdContext.has<String>("action") { action ->
            if (action.equals("status", true)) {
                adminStatus(context, musicManager)
            }
            shouldHalt = true
        }
        if (shouldHalt)
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