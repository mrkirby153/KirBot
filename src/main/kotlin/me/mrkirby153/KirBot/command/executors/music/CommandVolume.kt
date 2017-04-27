package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Message

@Command(name = "volume", description = "Change the volume of the robot", clearance = Clearance.BOT_MANAGER)
class CommandVolume : MusicCommand() {

    override fun exec(message: Message, args: Array<String>) {
        if(args.isEmpty()){
            message.send().error("Please specify a volume!").queue()
            return
        }

        val volume = Math.min(150, Math.max(args[0].toInt(), 0))

        server.musicManager.audioPlayer.volume = volume
        message.send().info("Set volume to __**$volume**__").queue()
    }
}