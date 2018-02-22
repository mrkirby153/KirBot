package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.MusicSettings
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.music.AudioTrackLoader
import me.mrkirby153.KirBot.utils.Context

@Command(name = "play", arguments = ["<query/url:string,rest>"])
class CommandPlay : BaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        if (context.kirbotGuild.musicManager.settings.enabled) {
            return
        } else {
            Bot.LOG.debug("Music is disabled in ${context.guild.id}, ignoring")
        }
        val data = cmdContext.get<String>("query/url") ?: ""

        val queuePosition =  -1 // TODO 12/17/2017 Reimplement QueueAt

        if (data.isBlank()) {
            if (context.kirbotGuild.musicManager.playing)
                throw CommandException("Already playing.")
            context.kirbotGuild.musicManager.audioPlayer.isPaused = false
            context.kirbotGuild.musicManager.manualPause = false
            context.channel.sendMessage(":arrow_forward: Music has been resumed").queue()
            return
        }

        if (!context.member.voiceState.inVoiceChannel()) {
            throw CommandException("Please join a voice channel first!")
        }

        if (context.guild.selfMember.voiceState.inVoiceChannel() && context.member.voiceState.channel != context.guild.selfMember.voiceState.channel) {
            throw CommandException("I am already playing music in **${context.guild.selfMember.voiceState.channel.name}**. Join me there!")
        }

        val musicSettings = context.kirbotGuild.musicManager.settings
        val restrictMode = musicSettings.whitelistMode
        val channels = musicSettings.channels.filter { it.isNotEmpty() }
        val currChannel = context.member.voiceState.channel

        when (restrictMode) {
            MusicSettings.WhitelistMode.WHITELIST -> {
                if (!channels.contains(currChannel.id))
                    throw CommandException("I cannot play music in your channel")
            }
            MusicSettings.WhitelistMode.BLACKLIST -> {
                if (channels.contains(currChannel.id))
                    throw CommandException("I cannot play music in your channel")
            }
            MusicSettings.WhitelistMode.OFF -> {
                // Do nothing
            }
        }
        var url = data.replace(Regex("&list=[A-Za-z0-9\\-+_]+"), "")
        if (!url.startsWith("http")) {
            val msg = context.channel.sendMessage(":mag: Searching YouTube for `$data`").complete()
            url = YoutubeSearch(data).execute()
            msg.delete().queue()
        }
        if (musicSettings.maxQueueLength != -1 && context.kirbotGuild.musicManager.queueLength() / (60 * 1000) >= musicSettings.maxQueueLength) {
            throw CommandException("The queue is too long right now, please try again when it is shorter")
        }
        Bot.playerManager.loadItem(url, AudioTrackLoader(context.kirbotGuild.musicManager, context.author, context, queuePosition))
    }
}