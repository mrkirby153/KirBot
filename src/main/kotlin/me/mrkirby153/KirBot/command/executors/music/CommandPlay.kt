package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.music.MusicLoadResultHandler
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.VoiceChannel
import java.awt.Color

@Command(name = "play", description = "Play some tunes")
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
        val url = args[0].replace(Regex("&list=[A-Za-z0-9\\-+_]+"), "")

        if (!(url.contains("youtu") || url.contains("vimeo") || url.contains("soundcloud"))) {
            message.send().embed("Error") {
                color = Color.RED
                description = "Please specify a valid URL"
                field("Acceptable Services", false, " - YouTube \n - Vimeo \n - Soundcloud")
            }.rest().queue()
            return
        }

        Bot.playerManager.loadItem(url, MusicLoadResultHandler(serverData, message.channel, { track ->
            if (track != null)
                message.send().embed("Music Queue") {
                    color = Color.CYAN
                    description = "Added __**[${track.info.title}](${track.info.uri})**__ to the queue!"
                }.rest().queue()
            if (!guild.selfMember.voiceState.inVoiceChannel()) {
                message.send().embed("Music") {
                    color = Color.CYAN
                    description = "Joined voice channel **${member.voiceState.channel.name}**"
                }.rest().queue()
                connectToVoice(member.voiceState.channel)
                // Start the music
                serverData.musicManager.trackScheduler.playNext()
            }
        }))
    }

    fun connectToVoice(channel: VoiceChannel) {
        serverData.musicManager.audioPlayer.volume = 100
        guild.audioManager.sendingHandler = serverData.musicManager.audioSender
        guild.audioManager.openAudioConnection(channel)
    }
}