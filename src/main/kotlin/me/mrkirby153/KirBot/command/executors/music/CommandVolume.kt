package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

class CommandVolume : MusicCommand() {

    override fun exec(context: Context, cmdContext: CommandContext) {
        val audioPlayer = context.data.musicManager.audioPlayer

        if (!cmdContext.has("volume")) {
            context.send().info("Current Volume: **${audioPlayer.volume}**").queue()
            return
        }

        var add = false
        var sub = false

        var num = cmdContext.string("volume") ?: "+0"

        if (num.startsWith("+")) {
            num = num.substring(1)
            add = true
        } else if (num.startsWith("-")) {
            num = num.substring(1)
            sub = true
        }

        val volume = Math.min(150, Math.max(num.toInt(), 0))

        if (add) {
            audioPlayer.volume += volume
        } else if (sub) {
            audioPlayer.volume -= volume
        } else {

            audioPlayer.volume = volume
        }
        context.send().info("Set volume to __**${audioPlayer.volume}**__").queue()
    }
}