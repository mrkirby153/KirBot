package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.utils.localizeTime
import net.dv8tion.jda.core.entities.MessageChannel
import java.awt.Color

class MusicLoadResultHandler(val server: ServerData, val channel: MessageChannel, val callback: (AudioTrack?) -> Unit) : AudioLoadResultHandler {

    override fun trackLoaded(p0: AudioTrack?) {
        if (p0 == null)
            return
        val musicData = server.getMusicData()
        if (!server.musicManager.adminOnly)
            if (musicData.maxSongLength != -1 && (p0.duration / 1000) / 60 > musicData.maxSongLength) {
                channel.send().error("That song is too long! The maximum length song is ${localizeTime(musicData.maxSongLength * 60)}").queue()
                return
            }
        server.musicManager.trackScheduler.queue.add(p0)
        callback.invoke(p0)
    }

    override fun noMatches() {
        channel.send().error("No matches were found for that song!").queue()
    }

    override fun playlistLoaded(p0: AudioPlaylist?) {
        val musicData = server.getMusicData()

        if (!server.musicManager.adminOnly) {
            if (!musicData.playlists) {
                channel.send().error("Playlist queueing is disabled!").queue()
                return
            }
            val playlistLength: Long = p0!!.tracks
                    .map { it.duration / 1000 }
                    .sum()
            if (musicData.maxSongLength != -1 && playlistLength / 60 > musicData.maxSongLength) {
                channel.send().error("That playlist is too long! The maximum length is ${localizeTime(musicData.maxSongLength * 60)}")
            }
        }
        server.musicManager.trackScheduler.queue.addAll(p0!!.tracks)
        channel.send().embed("Music Queue") {
            color = Color.CYAN
            description = "Queued all songs in **${p0.name}**!"
        }.rest().queue()
        callback.invoke(null)
    }

    override fun loadFailed(p0: FriendlyException?) {
        if (p0?.severity == FriendlyException.Severity.COMMON)
            channel.send().error("Loading failed: ${p0.localizedMessage}").queue()
    }
}