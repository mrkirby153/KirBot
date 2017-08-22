package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.mrkirby153.KirBot.redis.RedisConnector
import net.dv8tion.jda.core.entities.Guild
import org.json.JSONArray
import org.json.JSONObject

class TrackScheduler(val audioPlayer: AudioPlayer, val server: Guild) : AudioEventAdapter() {

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
            updateQueue()
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
        server.audioManager.closeAudioConnection()
        queue.clear()
        audioPlayer.stopTrack()
        playing = false
        nowPlaying = null
        resetRedis()
    }

    fun updateQueue() {
        RedisConnector.get().use { redis ->
            val json = JSONArray()
            this.queue.forEach {
                json.put(JSONObject().apply {
                    put("url", it.info.uri)
                    put("title", it.info.title)
                    put("duration", it.duration / 1000)
                })
            }
            redis.set("music.queue:${this.server.id}", json.toString())
            val finalNowPlaying = nowPlaying
            redis.set("music.playing:${this.server.id}", JSONObject().apply {
                if (finalNowPlaying != null) {
                    put("url", finalNowPlaying.info.uri)
                    put("title", finalNowPlaying.info.title)
                    put("duration", finalNowPlaying.duration / 1000)
                }
            }.toString())
        }
    }

    fun resetRedis() {
        RedisConnector.get().use { redis ->
            redis.del("music.playing:${this.server.id}")
            redis.del("music.queue:${this.server.id}")
        }
    }
}