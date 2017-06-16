package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command(name = "pause", clearance = Clearance.BOT_MANAGER, description = "Pauses the current song", category = "Music")
class CommandPause : MusicCommand() {
    override fun exec(context: Context, args: Array<String>) {
        if (!serverData.musicManager.trackScheduler.playing) {
            context.send().error("I'm not playing anything right now").queue()
            return
        }
        serverData.musicManager.trackScheduler.pause()
        context.send().info("Paused the music!").queue()
    }
}