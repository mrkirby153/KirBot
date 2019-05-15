package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context

@Command(name = "pause", clearance = CLEARANCE_MOD)
@CommandDescription("Pause the music that is currently playing")
class PauseCommand : MusicBaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        if (!manager.playing)
            throw CommandException("Music is already paused")
        manager.pause()
        context.channel.sendMessage(":pause_button: Music paused!").queue()
    }
}