package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command(name = "volume", description = "Change the volume of the robot", clearance = Clearance.BOT_MANAGER, category = "Music")
class CommandVolume : MusicCommand() {

    override fun exec(context: Context, args: Array<String>) {
        val audioPlayer = serverData.musicManager.audioPlayer
        if (args.isEmpty()) {
            context.send().info("Current Volume: **${audioPlayer.volume}**").queue()
            return
        }

        var add = false
        var sub = false

        var num = args[0]
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