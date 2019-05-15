package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.modules.music.MusicModule.Companion.alone
import me.mrkirby153.KirBot.modules.music.MusicModule.Companion.isDJ
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.SettingsRepository


class CommandDisconnect{

    @Command(name = "disconnect", category = CommandCategory.MUSIC)
    @CommandDescription("Disconnects the bot from the current voice channel")
    fun execute(context: Context, cmdContext: CommandContext) {
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        if (SettingsRepository.get(context.guild, "music_enabled", "0") == "0")
            return
        if(!isDJ(context.member)){
            if(!alone(context.member)){
                throw CommandException("You must be alone to use this command!")
            }
        }
        manager.disconnect()
        context.channel.sendMessage("Disconnected and cleared the queue").queue()
    }
}