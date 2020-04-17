package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.user.CLEARANCE_DEFAULT
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import javax.inject.Inject


class CommandDeQueue  @Inject constructor(private val musicModule: MusicModule){

    @Command(name = "dequeue", clearance = CLEARANCE_DEFAULT, arguments = ["<position:int>"], category = CommandCategory.MUSIC)
    @CommandDescription("Removes a previously queued song from the queue")
     fun execute(context: Context, cmdContext: CommandContext) {
        if (!GuildSettings.musicEnabled.get(context.guild))
            return
        val manager = musicModule.getManager(context.guild)
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