package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember

@Command(name = "connect,summon")
class CommandConnect : BaseCommand(CommandCategory.MUSIC) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (context.kirbotGuild.musicManager.settings.enabled) {
            return
        } else {
            Bot.LOG.debug("Music is disabled in ${context.guild.id}, ignoring")
        }
        val channel = context.author.getMember(context.guild).voiceState.channel ?: throw CommandException("Please join a voice channel first!")
        // Connect or summon
        if (!context.guild.selfMember.voiceState.inVoiceChannel()) {
            context.kirbotGuild.musicManager.audioPlayer.volume = 100
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