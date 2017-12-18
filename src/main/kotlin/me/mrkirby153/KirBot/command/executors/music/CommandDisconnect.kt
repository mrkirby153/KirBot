package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

@Command("disconnect,dc")
class CommandDisconnect: BaseCommand(CommandCategory.MUSIC) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.data.musicManager.disconnect()
        context.data.musicManager.queue.clear()
        context.data.musicManager.audioPlayer.playTrack(null)
        context.data.musicManager.resetQueue()
        context.send().success("Disconnected and cleared the queue :wave:").queue()
    }
}