package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape
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
            channel.sendMessage(embed(track.track.info.title) {
                color = Color.BLUE
                if(track.track.info.uri.contains("youtu")){
                    thumbnail ="https://i.ytimg.com/vi/${track.track.info.identifier}/default.jpg"
                }
                description {
                    +("**Now Playing**" link track.track.info.uri)
                    +"\n\n"
                    +track.track.info.title.mdEscape()
                    +"\n\nLength: `${MusicManager.parseMS(track.track.duration)}`"
                    +"\n\nRequested By: `${track.queuedBy.name}`"
                    val next = manager.queue.peekFirst()
                    +"\n\nUp Next: "
                    if (next == null)
                        +"Noting"
                    else
                        +"`${next.track.info.title}`"
                }
            }.build()).queue()
        } else {
            // Reset
            Bot.LOG.debug("[MUSIC/${manager.guild.id}] Queue is empty, shutting down")
            this.manager.disconnect()
        }
        manager.updateQueue()
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        Bot.LOG.debug("[MUSIC/${manager.guild.id}] Track is stuck, playing next song")
        playNextTrack()
    }
}