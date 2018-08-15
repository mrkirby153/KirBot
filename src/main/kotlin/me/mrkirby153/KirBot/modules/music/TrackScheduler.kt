package me.mrkirby153.KirBot.modules.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.mrkirby153.KirBot.Bot

class TrackScheduler(val manager: MusicManager) : AudioEventAdapter() {

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?,
                            endReason: AudioTrackEndReason?) {
        if (endReason != null)
            if (endReason.mayStartNext)
                manager.playNextTrack()
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        Bot.LOG.debug("[MUSIC/${manager.guild.id}] Track is stuck, playing next song")
        manager.playNextTrack()
    }
}