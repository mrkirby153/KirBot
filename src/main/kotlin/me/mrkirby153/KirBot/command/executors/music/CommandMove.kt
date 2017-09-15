package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link

class CommandMove : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val song = cmdContext.number("song") ?: throw CommandException("Please specify a number")

        val toPosition = cmdContext.number("position") ?: 0.0

        try {
            val max = Math.max(0.0, song - 1)
            val queuedSong = context.data.musicManager.queue.removeAt(max.toInt())
            context.data.musicManager.queue.add(Math.max(0, toPosition.toInt() - 1), queuedSong)
            context.send().embed {
                setDescription(buildString {
                    append("**${queuedSong.track.info.title}**" link queuedSong.track.info.uri)
                    if (queuedSong.track.info.uri.contains("youtu")) {
                        setThumbnail("https://i.ytimg.com/vi/${queuedSong.track.info.identifier}/default.jpg")
                    }
                    appendln("\nMoved `${queuedSong.track.info.title}` to position ${toPosition.toInt()} in the queue")
                })
            }.rest().queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException("There is nothing queued in that position")
        }
    }
}