package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember

class CommandConnect : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val channel = context.user.getMember(context.guild).voiceState.channel ?: throw CommandException("Please join a voice channel first!")
        // Connect or summon
        if (!context.guild.selfMember.voiceState.inVoiceChannel()) {
            context.data.musicManager.audioPlayer.volume = 100
            context.guild.audioManager.openAudioConnection(channel)
            context.channel.sendMessage(":white_check_mark: Joined the channel ${channel.name}").queue()
        } else {
            // Close the audio connection, and open a new one in the current channel
            context.guild.audioManager.openAudioConnection(channel)
            context.channel.sendMessage(":white_check_mark: Switched channels to ${channel.name}").queue()
        }
    }
}