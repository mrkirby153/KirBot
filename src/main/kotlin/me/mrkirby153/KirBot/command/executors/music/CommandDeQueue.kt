package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape
import net.dv8tion.jda.core.Permission

@Command(name = "dequeue", clearance = CLEARANCE_MOD, arguments = ["<position:int>"],
        permissions = [Permission.MESSAGE_EMBED_LINKS])
class CommandDeQueue : BaseCommand(CommandCategory.MUSIC) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!context.kirbotGuild.musicManager.settings.enabled) {
            return
        } else {
            Bot.LOG.debug("Music is disabled in ${context.guild.id}, ignoring")
        }
        val index = (cmdContext.get<Int>("position")?.toInt() ?: 1) - 1

        try {
            val song = context.kirbotGuild.musicManager.queue.removeAt(index)
            context.kirbotGuild.musicManager.updateQueue()
            context.send().embed {
                description {
                    +"**${song.track.info.title.mdEscape()}**" link song.track.info.uri
                    +"\nRemoved `${song.track.info.title}` from the queue"
                }
                if (song.track.info.uri.contains("youtu"))
                    thumbnail = "https://i.ytimg.com/vi/${song.track.info.identifier}/default.jpg"
                timestamp {
                    now()
                }
            }.rest().queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException(
                    "Position must be between 0 and ${context.kirbotGuild.musicManager.queue.size}")
        }
    }
}