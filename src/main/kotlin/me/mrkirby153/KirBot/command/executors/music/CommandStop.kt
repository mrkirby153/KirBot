package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.MusicSettings
import me.mrkirby153.KirBot.utils.Context
import java.awt.Color

class CommandStop : MusicCommand() {
    override fun exec(context: Context, cmdContext: CommandContext, musicData: MusicSettings) {
        context.data.musicManager.trackScheduler.reset()
        context.send().embed("Music"){
            setColor(Color.CYAN)
            setDescription("Stopped playing music and cleared the queue")
        }.rest().queue()
    }
}