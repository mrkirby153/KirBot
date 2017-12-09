package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape

class CommandDeQueue : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val index = (cmdContext.number("position") ?: 1.0) - 1

        try {
            val song = context.data.musicManager.queue.removeAt(index.toInt())
            context.data.musicManager.updateQueue()
            context.send().embed {
                description {
                    +"**${song.track.info.title.mdEscape()}**" link song.track.info.uri
                    +"\nRemoved `${song.track.info.title}` from the queue"
                }
                if (song.track.info.uri.contains("youtu"))
                    thumbnail = "https://i.ytimg.com/vi/${song.track.info.identifier}/default.jpg"
            }.rest().queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException("Position must be between 0 and ${context.data.musicManager.queue.size}")
        }
    }
}