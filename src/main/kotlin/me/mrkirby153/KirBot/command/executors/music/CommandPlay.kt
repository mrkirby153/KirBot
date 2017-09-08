package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.MusicSettings
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.music.MusicLoadResultHandler
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.entities.VoiceChannel
import java.awt.Color

class CommandPlay : MusicCommand() {

    override fun exec(context: Context, cmdContext: CommandContext, musicData: MusicSettings) {
        val member = context.member

        val guild = context.guild

        if (guild.selfMember.voiceState.inVoiceChannel() && member.voiceState.channel != guild.selfMember.voiceState.channel) {
//            context.send().error("I'm already playing music in **${guild.selfMember.voiceState.channel.name}**. Join me there").queue()
            throw CommandException("I am already playing music in **${guild.selfMember.voiceState.channel.name}**. Join me there")
        }

        if (!member.voiceState.inVoiceChannel()) {
            throw CommandException("Please join a voice channel first!")
        }

        // Check context blacklist/whitelist
        val restrictMode = musicData.whitelistMode

        val channels = musicData.channels.filter { it.isNotEmpty() }
        val currentChannel = member.voiceState.channel

        when (restrictMode) {
            MusicSettings.WhitelistMode.WHITELIST -> {
                if (!channels.contains(currentChannel.id)) {
                    throw CommandException("I cannot play music in your channel!")
                }
            }
            MusicSettings.WhitelistMode.BLACKLIST -> {
                if (channels.contains(currentChannel.id)) {
                    throw CommandException("I cannot play music in your channel!")
                }
            }
            MusicSettings.WhitelistMode.OFF -> {
                // Do nothing, whitelisting disabled
            }
        }

        val data = cmdContext.string("data") ?: ""

        if (data.isEmpty()) {
            if (!context.data.musicManager.trackScheduler.playing) {
                context.data.musicManager.trackScheduler.resume()
                context.send().embed("Music") {
                    setColor(Color.CYAN)
                    setDescription("Resumed!")
                }.rest().queue()
                return
            }

            throw CommandException("Please specify something to play!")
        }

        // Remove playlist from URL to prevent accidental queueing of playlists
        var url = data.replace(Regex("&list=[A-Za-z0-9\\-+_]+"), "")
        if (!(url.contains("youtu") || url.contains("vimeo") || url.contains("soundcloud"))) {
            Bot.LOG.debug("Searching youtube for \"$data\"")
            url = YoutubeSearch(data).execute()
        }

        // Check track blacklist
        if (!context.data.musicManager.adminOnly)
            if (musicData.blacklistedSongs.isNotEmpty())
                musicData.blacklistedSongs.filter { it.isNotEmpty() }
                        .filter { it in url }
                        .forEach { throw CommandException("You cannot play this track at this time!") }

        // Check queue length
        if (!context.data.musicManager.adminOnly)
            if (musicData.maxQueueLength != -1 && context.data.musicManager.trackScheduler.queueLength() / 60 >= musicData.maxQueueLength) {
                context.send().error("The queue is too long right now, please try again when it is shorter.").queue()
                return
            }

        Bot.playerManager.loadItem(url, MusicLoadResultHandler(context.data, context, { track ->
            if (track != null) {
                context.send().embed("Music Queue") {
                    setColor(Color.CYAN)
                    setDescription("Added __**[${track.info.title}](${track.info.uri})**__ to the queue!")
                }.rest().queue()
                connectToVoice(member.voiceState.channel, context)
            } else {
                // Playlist queued
                if (context.data.musicManager.trackScheduler.queueLength() > 0) {
                    connectToVoice(member.voiceState.channel, context)
                }
            }
        }))
    }

    fun connectToVoice(channel: VoiceChannel, context: Context) {
        val guild = context.guild
        if (!guild.selfMember.voiceState.inVoiceChannel()) {
            val data = context.data
            data.musicManager.audioPlayer.volume = 100
            guild.audioManager.sendingHandler = data.musicManager.audioSender
            guild.audioManager.openAudioConnection(channel)
            context.send().embed("Music") {
                setColor(Color.CYAN)
                setDescription("Joined voice context **${channel.name}**")
            }.rest().queue()
            // Start the music
            data.musicManager.trackScheduler.playNext()
        }
    }
}