package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import java.awt.Color

@Command(name = "stop", description = "Stops and clears the music queue", clearance = Clearance.BOT_MANAGER, category = "Music")
class CommandStop : MusicCommand() {
    override fun exec(context: Context, args: Array<String>) {
        serverData.musicManager.trackScheduler.reset()
        context.send().embed("Music"){
            setColor(Color.CYAN)
            setDescription("Stopped playing music and cleared the queue")
        }.rest().queue()
    }
}