package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Message

@Command(name = "volume", description = "Change the volume of the robot", clearance = Clearance.BOT_MANAGER)
class CommandVolume : MusicCommand() {

    override fun exec(message: Message, args: Array<String>) {
        val audioPlayer = server.musicManager.audioPlayer
        if (args.isEmpty()) {
            message.send().info("Current Volume: **${audioPlayer.volume}**").queue()
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
        message.send().info("Set volume to __**${audioPlayer.volume}**__").queue()
    }
}