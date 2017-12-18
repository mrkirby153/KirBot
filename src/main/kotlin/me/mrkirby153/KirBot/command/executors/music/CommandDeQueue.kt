package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.*
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape

@Command("dequeue")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandDeQueue : BaseCommand(CommandCategory.MUSIC, Arguments.number("position", min = 1)) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val index = (cmdContext.get<Double>("position")?.toInt() ?: 1) - 1

        try {
            val song = context.data.musicManager.queue.removeAt(index)
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