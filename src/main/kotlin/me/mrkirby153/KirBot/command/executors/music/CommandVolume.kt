package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.settings.GuildSettings


class CommandVolume {

    @Command(name = "volume", clearance = CLEARANCE_MOD, arguments = ["[volume:string]"], category = CommandCategory.MUSIC)
    @CommandDescription("Sets the bot's volume")
    fun execute(context: Context, cmdContext: CommandContext) {
        if (!GuildSettings.musicEnabled.get(context.guild))
            return
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        val audioPlayer = manager.audioPlayer

        if (!cmdContext.has("volume")) {
            context.send().info("Current Volume: **${audioPlayer.volume}**").queue()
            return
        }

        var add = false
        var sub = false

        var num = cmdContext.get<String>("volume") ?: "+0"

        if (num.startsWith("+")) {
            num = num.substring(1)
            add = true
        } else if (num.startsWith("-")) {
            num = num.substring(1)
            sub = true
        }

        val volume = Math.min(150, Math.max(num.toInt(), 0))

        when {
            add -> audioPlayer.volume += volume
            sub -> audioPlayer.volume -= volume
            else -> audioPlayer.volume = volume
        }
        context.send().info("Set volume to __**${audioPlayer.volume}**__").queue()
    }
}