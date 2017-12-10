package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.MusicSettings
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.music.AudioTrackLoader
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.utils.Context

class CommandPlay : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val data = cmdContext.string("query/url") ?: ""

        val queuePosition = cmdContext.number("position")?.toInt() ?: -1

        if (data.isBlank()) {
            if (context.data.musicManager.playing)
                throw CommandException("Already playing.")
            context.data.musicManager.audioPlayer.isPaused = false
            context.data.musicManager.manualPause = false
            context.channel.sendMessage(":arrow_forward: Music has been resumed").queue()
            return
        }

        if (!context.member.voiceState.inVoiceChannel()) {
            throw CommandException("Please join a voice channel first!")
        }

        if (context.guild.selfMember.voiceState.inVoiceChannel() && context.member.voiceState.channel != context.guild.selfMember.voiceState.channel) {
            throw CommandException("I am already playing music in **${context.guild.selfMember.voiceState.channel.name}**. Join me there!")
        }

        val musicSettings = MusicManager.musicSettings[context.guild.id] ?: throw CommandException("Error retrieving music settings")
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
        if (musicSettings.maxQueueLength != -1 && context.data.musicManager.queueLength() / (60 * 1000) >= musicSettings.maxQueueLength) {
            throw CommandException("The queue is too long right now, please try again when it is shorter")
        }
        Bot.playerManager.loadItem(url, AudioTrackLoader(context.data.musicManager, context.author, context, queuePosition))
    }
}