package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.mrkirby153.KirBot.data.ServerData
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import java.awt.Color

class MusicLoadResultHandler(val server: ServerData, val channel: MessageChannel, val callback: (AudioTrack?) -> Unit) : AudioLoadResultHandler {

    override fun trackLoaded(p0: AudioTrack?) {
        if (p0 == null)
            return
        server.musicManager.trackScheduler.queue.add(p0)
        callback.invoke(p0)
    }

    override fun noMatches() {
        channel.send().error("No matches were found for that song!").queue()
    }

    override fun playlistLoaded(p0: AudioPlaylist?) {
//        channel.send().error("Playlists are not supported right now")
        for (song in p0!!.tracks) {
            server.musicManager.trackScheduler.queue.add(song)
        }
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