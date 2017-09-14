package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.link
import java.awt.Color

class TrackScheduler(val manager: MusicManager) : AudioEventAdapter() {

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        if (endReason != null)
            if (endReason.mayStartNext)
                playNextTrack()
    }

    fun playNextTrack() {
        if (manager.queue.peekFirst() != null) {
            Bot.LOG.debug("[MUSIC/${manager.guild.id}] Playing next track...")
            val track = manager.queue.removeFirst()
            manager.audioPlayer.playTrack(track.track)
            // Announce
            val channel = manager.guild.getTextChannelById(track.queuedIn) ?: return
            channel.sendMessage(embed {
                setColor(Color.BLUE)
                setDescription(buildString {
                    append("**Now Playing**" link track.track.info.uri)
                    append("\n\n")
                    append(track.track.info.title)
                    append("\n\nLength: `${MusicManager.parseMS(track.track.duration)}`")
                    append("\n\nRequested By: `${track.queuedBy.name}`")
                    val next = manager.queue.peekFirst()
                    append("\n\nUp Next: ")
                    if (next == null)
                        append("Noting")
                    else
                        append("`${next.track.info.title}`")
                })
            }.build()).queue()
        } else {
            // Reset
            Bot.LOG.debug("[MUSIC/${manager.guild.id}] Queue is empty, shutting down")
        }
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        Bot.LOG.debug("[MUSIC/${manager.guild.id}] Track is stuck, playing next song")
        playNextTrack()
    }
}