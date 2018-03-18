package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape

@Command(name = "move", arguments = ["<from:int,0,x>", "[to:int,0,x]"])
class CommandMove : BaseCommand(CommandCategory.MUSIC) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!context.kirbotGuild.musicManager.settings.enabled) {
            return
        } else {
            Bot.LOG.debug("Music is disabled in ${context.guild.id}, ignoring")
        }

        val song = cmdContext.get<Double>("from")?.toInt() ?: throw CommandException("Please specify a number")

        val toPosition = cmdContext.get<Double>("to")?.toInt() ?: 0
        try {
            val max = Math.max(0, song - 1)
            val queuedSong = context.kirbotGuild.musicManager.queue.removeAt(max)
            context.kirbotGuild.musicManager.queue.add(Math.max(0, toPosition - 1), queuedSong)
            context.kirbotGuild.musicManager.updateQueue()
            context.send().embed {
                description {
                    +"**${queuedSong.track.info.title.mdEscape()}**" link queuedSong.track.info.uri
                    +"\nMoved `${queuedSong.track.info.title}` to position ${toPosition} in the queue"
                }
                if (queuedSong.track.info.uri.contains("youtu")) {
                    thumbnail = "https://i.ytimg.com/vi/${queuedSong.track.info.identifier}/default.jpg"
                }
                timestamp {
                    now()
                }
            }.rest().queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException("There is nothing queued in that position")
        }
    }
}