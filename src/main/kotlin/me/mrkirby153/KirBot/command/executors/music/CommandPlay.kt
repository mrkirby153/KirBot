package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.music.MusicData
import me.mrkirby153.KirBot.music.MusicLoadResultHandler
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.VoiceChannel
import java.awt.Color

@Command(name = "play", description = "Play some tunes", category = "Music")
class CommandPlay : MusicCommand() {

    override fun exec(message: Message, args: Array<String>) {
        val member = guild.getMember(message.author) ?: return

        if (guild.selfMember.voiceState.inVoiceChannel() && member.voiceState.channel != guild.selfMember.voiceState.channel) {
            message.send().error("I'm already playing music in **${guild.selfMember.voiceState.channel.name}**. Join me there").queue()
            return
        }

        if (!member.voiceState.inVoiceChannel()) {
            message.send().error("Please join a voice chat first!").queue()
            return
        }

        // Check channel blacklist/whitelist
        val musicData = serverData.getMusicData()
        val restrictMode = musicData.whitelistMode

        val channels = musicData.channels.filter { it.isNotEmpty() }
        val currentChannel = member.voiceState.channel

        when (restrictMode) {
            MusicData.WhitelistMode.WHITELIST -> {
                if (!channels.contains(currentChannel.id)) {
                    message.send().error("I cannot play music in your channel!").queue()
                    return
                }
            }
            MusicData.WhitelistMode.BLACKLIST -> {
                if (channels.contains(currentChannel.id)) {
                    message.send().error("I cannot play music in your channel!").queue()
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
                message.send().embed("Music") {
                    color = Color.CYAN
                    description = "Resumed!"
                }
                return
            }

            message.send().embed("Error") {
                color = Color.RED
                description = "Please specify something to play!"
            }.rest().queue()
        }

        // Remove playlist from URL to prevent accidental queueing of playlists
        var url = args[0].replace(Regex("&list=[A-Za-z0-9\\-+_]+"), "")

        if (!(url.contains("youtu") || url.contains("vimeo") || url.contains("soundcloud"))) {
/*            message.send().embed("Error") {
                color = Color.RED
                description = "Please specify a valid URL"
                field("Acceptable Services", false, " - YouTube \n - Vimeo \n - Soundcloud")
            }.rest().queue()*/
            println("Searching youtube.")
            url = YoutubeSearch(args.joinToString(" ")).execute()
        }

        // Check track blacklist
        if (!serverData.musicManager.adminOnly)
            if (musicData.blacklistedSongs.isNotEmpty())
                for (blacklistedTrack in musicData.blacklistedSongs.filter { it.isNotEmpty() }) {
                    if (blacklistedTrack in url) {
                        message.send().embed("Error") {
                            color = Color.RED
                            description = "You cannot play this track at this time"
                        }.rest().queue()
                        return
                    }
                }

        // Check queue length
        if (!serverData.musicManager.adminOnly)
            if (musicData.maxQueueLength != -1 && serverData.musicManager.trackScheduler.queueLength() / 60 >= musicData.maxQueueLength) {
                message.send().error("The queue is too long right now, please try again when it is shorter.").queue()
                return
            }

        Bot.playerManager.loadItem(url, MusicLoadResultHandler(serverData, message.channel, { track ->
            if (track != null) {
                message.send().embed("Music Queue") {
                    color = Color.CYAN
                    description = "Added __**[${track.info.title}](${track.info.uri})**__ to the queue!"
                }.rest().queue()
                connectToVoice(member.voiceState.channel, message)
            } else {
                // Playlist queued
                if (serverData.musicManager.trackScheduler.queueLength() > 0) {
                    connectToVoice(member.voiceState.channel, message)
                }
            }
        }))
    }

    fun connectToVoice(channel: VoiceChannel, message: Message) {
        if (!guild.selfMember.voiceState.inVoiceChannel()) {
            serverData.musicManager.audioPlayer.volume = 100
            guild.audioManager.sendingHandler = serverData.musicManager.audioSender
            guild.audioManager.openAudioConnection(channel)
            message.send().embed("Music") {
                color = Color.CYAN
                description = "Joined voice channel **${channel.name}**"
            }.rest().queue()
            // Start the music
            serverData.musicManager.trackScheduler.playNext()
        }
    }
}