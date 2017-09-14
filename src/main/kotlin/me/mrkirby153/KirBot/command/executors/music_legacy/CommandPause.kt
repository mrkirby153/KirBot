package me.mrkirby153.KirBot.command.executors.music_legacy

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.MusicSettings
import me.mrkirby153.KirBot.utils.Context

class CommandPause : MusicCommand() {
    override fun exec(context: Context, cmdContext: CommandContext, musicData: MusicSettings) {
        if (!context.data.musicManager_old.trackScheduler.playing) {
            throw CommandException("I'm not playing anything right now")
        }
        context.data.musicManager_old.trackScheduler.pause()
        context.send().info("Paused the music!").queue()
    }
}