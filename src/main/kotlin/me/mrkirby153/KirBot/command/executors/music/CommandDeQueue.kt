package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link

class CommandDeQueue : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val index = (cmdContext.number("position") ?: 1.0) - 1

        try {
            val song = context.data.musicManager.queue.removeAt(index.toInt())
            context.send().embed {
                setDescription(buildString {
                    append("**${song.track.info.title}**" link song.track.info.uri)
                    if (song.track.info.uri.contains("youtu")) {
                        setThumbnail("https://i.ytimg.com/vi/${song.track.info.identifier}/default.jpg")
                    }
                    appendln("\nRemoved `${song.track.info.title}` from the queue")
                })
            }.rest().queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException("Position must be between 0 and ${context.data.musicManager.queue.size}")
        }
    }
}