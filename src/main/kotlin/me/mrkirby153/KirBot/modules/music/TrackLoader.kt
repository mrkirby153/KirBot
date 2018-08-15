package me.mrkirby153.KirBot.modules.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.entities.Message

class TrackLoader(val manager: MusicManager, val context: Context, val msg: Message) :
        AudioLoadResultHandler {

    override fun loadFailed(exception: FriendlyException) {
        msg.editMessage(
                "$RED_TICK An error occurred when loading the track: `${exception.message}`").queue()
    }

    override fun trackLoaded(track: AudioTrack) {
        if (manager.settings.maxSongLength != -1) {
            if (track.duration / (60 * 1000) > manager.settings.maxSongLength) {
                msg.editMessage(
                        "$RED_TICK The requested song is too long. The maximum song length is **${Time.format(
                                1, manager.settings.maxSongLength * 60 * 1000L)}**").queue()
            }
        }
        manager.queue(track, context.author)
        msg.editMessage("$GREEN_TICK Added `${track.info.title}` [${MusicManager.parseMS(
                track.duration)}] to the queue at position **${manager.queue.size}**").queue()
        // Auto start the music
        if (manager.nowPlaying == null || !manager.playing) {
            manager.playNextTrack()
        }
    }

    override fun noMatches() {
        msg.editMessage("$RED_TICK No matches were found!").queue()
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        if (!manager.settings.playlists) {
            msg.editMessage("$RED_TICK Queueing playlists are disabled! Please queue songs individually").queue()
        }
        playlist.tracks.forEach {
            manager.queue(it, context.author)
        }
        msg.editMessage(
                "$GREEN_TICK Added **${playlist.tracks.size}** tracks to the queue from the playlist `${playlist.name}`").queue()
    }
}