package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context

@Command(name = "volume", clearance = CLEARANCE_MOD, arguments = ["[volume:string]"])
class CommandVolume : MusicBaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
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