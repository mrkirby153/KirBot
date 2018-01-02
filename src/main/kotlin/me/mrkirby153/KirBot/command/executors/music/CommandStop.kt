package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command("stop,pause")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandStop : MusicCommand() {

    override fun exec(context: Context, cmdContext: CommandContext) {
        if (!context.data.musicManager.playing) {
            throw CommandException("Music is already stopped")
        }
        context.data.musicManager.audioPlayer.isPaused = true
        context.data.musicManager.manualPause = true
        context.channel.sendMessage(":pause_button: Music has been paused!").queue()
    }
}