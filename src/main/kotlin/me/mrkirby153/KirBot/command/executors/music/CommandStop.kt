package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Message
import java.awt.Color

@Command(name = "stop", description = "Stops and clears the music queue", clearance = Clearance.BOT_MANAGER, category = "Music")
class CommandStop : MusicCommand() {
    override fun exec(message: Message, args: Array<String>) {
        serverData.musicManager.trackScheduler.reset()
        message.send().embed("Music"){
            color = Color.CYAN
            description = "Stopped playing music and cleared the queue"
        }.rest().queue()
    }
}