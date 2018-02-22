package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command(name = "stop,pause", clearance = Clearance.BOT_MANAGER)
class CommandStop : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!context.kirbotGuild.musicManager.playing) {
            throw CommandException("Music is already stopped")
        }
        context.kirbotGuild.musicManager.audioPlayer.isPaused = true
        context.kirbotGuild.musicManager.manualPause = true
        context.channel.sendMessage(":pause_button: Music has been paused!").queue()
    }
}