package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.user.CLEARANCE_DEFAULT
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.Permission

@Command(name = "dequeue", clearance = CLEARANCE_DEFAULT, arguments = ["<position:int>"],
        permissions = [Permission.MESSAGE_EMBED_LINKS])
@CommandDescription("Removes a previously queued song from the queue")
class CommandDeQueue : MusicBaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        val index = (cmdContext.get<Int>("position")?.toInt() ?: 1) - 1
        try {
            val song = manager.queue[index]
            if (song.queuedBy != context.author) {
                if (context.author.getClearance(context.guild) < CLEARANCE_MOD)
                    throw CommandException("You cannot dequeue that song")
            }
            manager.queue.removeAt(index)
            context.send().success("Removed `${song.track.info.title}` from the queue!").queue()
        } catch (e: IndexOutOfBoundsException) {
            throw CommandException(
                    "Position must be between 1 and ${manager.queue.size + 1}")
        }
    }
}