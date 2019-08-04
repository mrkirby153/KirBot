package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.SettingsRepository


class PauseCommand {

    @Command(name = "pause", clearance = CLEARANCE_MOD, category = CommandCategory.MUSIC)
    @CommandDescription("Pause the music that is currently playing")
    fun execute(context: Context, cmdContext: CommandContext) {
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        if (SettingsRepository.get(context.guild, "music_enabled", "0") == "0")
            return
        if (!manager.playing)
            throw CommandException("Music is already paused")
        manager.pause()
        context.channel.sendMessage(":pause_button: Music paused!").queue()
    }
}