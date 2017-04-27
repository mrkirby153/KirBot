package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.mrkirby153.KirBot.server.Server

class TrackScheduler(val audioPlayer: AudioPlayer, val server: Server) : AudioEventAdapter() {

    val queue = mutableListOf<AudioTrack>()

    var nowPlaying: AudioTrack? = null

    var playing = false

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        if (endReason!!.mayStartNext)
            playNext()
    }

    fun playNext() {
        if (!queue.isEmpty()) {
            val track = queue.removeAt(0)
            audioPlayer.playTrack(track)
            nowPlaying = track
            playing = true
        } else {
           reset()
        }
    }

    fun queueLength(): Int {
        val duration = queue.sumBy { (it.duration / 1000).toInt() }
        return duration
    }

    fun pause() {
        audioPlayer.isPaused = true
        this.playing = false
    }

    fun resume() {
        audioPlayer.isPaused = false
        this.playing = true
    }

    fun setVolume(volume: Int) {
        audioPlayer.volume = volume
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        playNext()
    }

    fun reset() {
        server.guild.audioManager.closeAudioConnection()
        queue.clear()
        audioPlayer.stopTrack()
        playing = false
        nowPlaying = null
    }
}