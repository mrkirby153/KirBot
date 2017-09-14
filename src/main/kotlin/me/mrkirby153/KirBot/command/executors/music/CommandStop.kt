package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context

class CommandStop : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!context.data.musicManager.playing) {
            throw CommandException("Music is already stopped")
        }
        context.data.musicManager.audioPlayer.isPaused = true
        context.channel.sendMessage(":pause_button: Music has been paused!").queue()
    }
}