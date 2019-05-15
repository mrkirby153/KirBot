package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.SettingsRepository
import net.dv8tion.jda.core.Permission


class CommandMove {

    @Command(name = "move", arguments = ["<from:int>", "[to:int]"],
            permissions = [Permission.MESSAGE_EMBED_LINKS], category = CommandCategory.MUSIC)
    @CommandDescription("Move songs around in the queue")
    fun execute(context: Context, cmdContext: CommandContext) {
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        if (SettingsRepository.get(context.guild, "music_enabled", "0") == "0")
            return
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