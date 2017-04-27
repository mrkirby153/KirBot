package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Message

@Command(name = "pause", clearance = Clearance.BOT_MANAGER, description = "Pauses the current song")
class CommandPause : MusicCommand() {
    override fun exec(message: Message, args: Array<String>) {
        if (!server.musicManager.trackScheduler.playing) {
            message.send().error("I'm not playing anything right now").queue()
            return
        }
        server.musicManager.trackScheduler.pause()
        message.send().info("Paused the music!").queue()
    }
}