package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.music.MusicData
import me.mrkirby153.KirBot.music.MusicLoadResultHandler
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.entities.VoiceChannel
import java.awt.Color

@Command(name = "play", description = "Play some tunes", category = "Music")
class CommandPlay : MusicCommand() {

    override fun exec(context: Context, args: Array<String>) {
        val member = guild.getMember(context.author) ?: return

        if (guild.selfMember.voiceState.inVoiceChannel() && member.voiceState.channel != guild.selfMember.voiceState.channel) {
            context.send().error("I'm already playing music in **${guild.selfMember.voiceState.channel.name}**. Join me there").queue()
            return
        }

        if (!member.voiceState.inVoiceChannel()) {
            context.send().error("Please join a voice chat first!").queue()
            return
        }

        // Check context blacklist/whitelist
        val musicData = serverData.getMusicData()
        val restrictMode = musicData.whitelistMode

        val channels = musicData.channels.filter { it.isNotEmpty() }
        val currentChannel = member.voiceState.channel

        when (restrictMode) {
            MusicData.WhitelistMode.WHITELIST -> {
                if (!channels.contains(currentChannel.id)) {
                    context.send().error("I cannot play music in your context!").queue()
                    return
                }
            }
            MusicData.WhitelistMode.BLACKLIST -> {
                if (channels.contains(currentChannel.id)) {
                    context.send().error("I cannot play music in your context!").queue()
                    return
                }
            }
            MusicData.WhitelistMode.OFF -> {
                // Do nothing, whitelisting disabled
            }
        }

        if (args.isEmpty()) {
            if (!serverData.musicManager.trackScheduler.playing) {
                serverData.musicManager.trackScheduler.resume()
                context.send().embed("Music") {
                    setColor(Color.CYAN)
                    setDescription("Resumed!")
                }.rest().queue()
                return
            }

            context.send().error("Please specify something to play!").queue()
        }

        // Remove playlist from URL to prevent accidental queueing of playlists
        var url = args[0].replace(Regex("&list=[A-Za-z0-9\\-+_]+"), "")
        if (!(url.contains("youtu") || url.contains("vimeo") || url.contains("soundcloud"))) {
            println("Searching youtube.")
            url = YoutubeSearch(args.joinToString(" ")).execute()
        }

        // Check track blacklist
        if (!serverData.musicManager.adminOnly)
            if (musicData.blacklistedSongs.isNotEmpty())
                for (blacklistedTrack in musicData.blacklistedSongs.filter { it.isNotEmpty() }) {
                    if (blacklistedTrack in url) {
                        context.send().error("You cannot play this track at this time").queue()
                        return
                    }
                }

        // Check queue length
        if (!serverData.musicManager.adminOnly)
            if (musicData.maxQueueLength != -1 && serverData.musicManager.trackScheduler.queueLength() / 60 >= musicData.maxQueueLength) {
                context.send().error("The queue is too long right now, please try again when it is shorter.").queue()
                return
            }

        Bot.playerManager.loadItem(url, MusicLoadResultHandler(serverData, context, { track ->
            if (track != null) {
                context.send().embed("Music Queue") {
                    setColor(Color.CYAN)
                    setDescription("Added __**[${track.info.title}](${track.info.uri})**__ to the queue!")
                }.rest().queue()
                connectToVoice(member.voiceState.channel, context)
            } else {
                // Playlist queued
                if (serverData.musicManager.trackScheduler.queueLength() > 0) {
                    connectToVoice(member.voiceState.channel, context)
                }
            }
        }))
    }

    fun connectToVoice(channel: VoiceChannel, context: Context) {
        if (!guild.selfMember.voiceState.inVoiceChannel()) {
            serverData.musicManager.audioPlayer.volume = 100
            guild.audioManager.sendingHandler = serverData.musicManager.audioSender
            guild.audioManager.openAudioConnection(channel)
            context.send().embed("Music") {
                setColor(Color.CYAN)
                setDescription("Joined voice context **${channel.name}**")
            }.rest().queue()
            // Start the music
            serverData.musicManager.trackScheduler.playNext()
        }
    }
}