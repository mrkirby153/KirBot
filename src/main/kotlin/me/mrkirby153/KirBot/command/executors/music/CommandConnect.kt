package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember

@Command("connect,summon")
class CommandConnect : MusicCommand() {

    override fun exec(context: Context, cmdContext: CommandContext) {
        val channel = context.author.getMember(context.guild).voiceState.channel ?: throw CommandException("Please join a voice channel first!")
        // Connect or summon
        if (!context.guild.selfMember.voiceState.inVoiceChannel()) {
            context.data.musicManager.audioPlayer.volume = 100
            context.guild.audioManager.openAudioConnection(channel)
            context.success()
        } else {
            // TODO 12/17/2017 Restrict moving if playing in a different channel
            // Close the audio connection, and open a new one in the current channel
            context.guild.audioManager.openAudioConnection(channel)
            context.success()
        }
    }
}