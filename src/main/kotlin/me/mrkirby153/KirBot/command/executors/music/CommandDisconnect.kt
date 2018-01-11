package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.embed
import java.awt.Color

@Command("disconnect,dc")
class CommandDisconnect : MusicCommand() {

    override fun exec(context: Context, cmdContext: CommandContext) {
        context.kirbotGuild.musicManager.disconnect()
        context.kirbotGuild.musicManager.queue.clear()
        context.kirbotGuild.musicManager.audioPlayer.playTrack(null)
        context.kirbotGuild.musicManager.resetQueue()
        context.channel.sendMessage(embed("Success") {
            description { +"Disconnected and cleared the queue :wave:" }
            timestamp { now() }
            color = Color.GREEN
        }.build()).queue()
    }
}