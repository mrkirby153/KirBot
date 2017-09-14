package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context

class CommandDisconnect: CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        context.data.musicManager.disconnect()
        context.data.musicManager.queue.clear()
        context.data.musicManager.audioPlayer.playTrack(null)
        context.send().success("Disconnected and cleared the queue :wave:").queue()
    }
}