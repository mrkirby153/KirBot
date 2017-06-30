package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

class CommandPause : MusicCommand() {
    override fun exec(context: Context, cmdContext: CommandContext) {
        if (!context.data.musicManager.trackScheduler.playing) {
            throw CommandException("I'm not playing anything right now")
        }
        context.data.musicManager.trackScheduler.pause()
        context.send().info("Paused the music!").queue()
    }
}