package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.*
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape

@Command("move")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandMove : BaseCommand(CommandCategory.MUSIC, Arguments.number("from", min = 0), Arguments.number("to", false, min = 0)) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val song = cmdContext.get<Double>("from")?.toInt() ?: throw CommandException("Please specify a number")

        val toPosition = cmdContext.get<Double>("to")?.toInt() ?: 0
        try {
            val max = Math.max(0, song - 1)
            val queuedSong = context.data.musicManager.queue.removeAt(max)
            context.data.musicManager.queue.add(Math.max(0, toPosition - 1), queuedSong)
            context.data.musicManager.updateQueue()
            context.send().embed {
                description {
                    +"**${queuedSong.track.info.title.mdEscape()}**" link queuedSong.track.info.uri
                    +"\nMoved `${queuedSong.track.info.title}` to position ${toPosition} in the queue"
                }
                if (queuedSong.track.info.uri.contains("youtu")) {
                    thumbnail = "https://i.ytimg.com/vi/${queuedSong.track.info.identifier}/default.jpg"
                }
            }.rest().queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException("There is nothing queued in that position")
        }
    }
}