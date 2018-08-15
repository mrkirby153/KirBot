package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.Permission

@Command(name = "move", arguments = ["<from:int>", "[to:int]"],
        permissions = [Permission.MESSAGE_EMBED_LINKS])
class CommandMove : MusicBaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        val song = cmdContext.get<Int>("from")?.toInt()!!

        val toPosition = cmdContext.get<Int>("to")?.toInt() ?: 0
        try {
            val max = Math.max(0, song - 1)
            val queuedSong = manager.queue.removeAt(max)
            manager.queue.add(Math.max(0, toPosition - 1), queuedSong)
            context.send().success(
                    "Moved `${queuedSong.track.info.title}` to position **$toPosition**").queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException("There is nothing queued in that position")
        }
    }
}