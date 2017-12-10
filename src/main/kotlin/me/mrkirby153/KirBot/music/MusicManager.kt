package me.mrkirby153.KirBot.music

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.MusicSettings
import me.mrkirby153.KirBot.redis.RedisConnector
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList
import java.util.concurrent.TimeUnit

class MusicManager(val guild: Guild) {

    val audioPlayer: AudioPlayer = Bot.playerManager.createPlayer()

    private val sender = AudioPlayerSendHandler(audioPlayer)

    val queue = LinkedList<QueuedSong>()

    val nowPlaying: AudioTrack?
        get() = audioPlayer.playingTrack

    val playing: Boolean
        get() = !audioPlayer.isPaused

    val trackScheduler = TrackScheduler(this)

    var boundChannel : String? = null

    var nowPlayingMessage: Message? = null

    init {
        guild.audioManager.sendingHandler = sender
        audioPlayer.addListener(trackScheduler)
    }

    fun disconnect() {
        guild.audioManager.closeAudioConnection()
        audioPlayer.playTrack(null)
        boundChannel = null
        nowPlayingMessage?.delete()?.queue()
        resetQueue()
    }

    fun getBoundChannel() : TextChannel? {
        if(this.boundChannel == null)
            return null
        return guild.getTextChannelById(this.boundChannel)
    }

    fun queueLength(): Long {
        var length = 0L
        queue.forEach {
            length += it.track.duration
        }
        return length
    }

    fun updateQueue() {
        // Update the queue in redis
        val json = JSONArray()
        this.queue.forEach {
            json.put(serializeQueuedSong(it))
        }
        RedisConnector.get().use {
            it.set("music.queue:${this.guild.id}", json.toString())
            val playing = nowPlaying
            if (playing != null) {
                it.set("music.playing:${this.guild.id}", serializeQueuedSong(playing).toString())
            } else {
                it.del("music.playing:${this.guild.id}")
            }
        }
    }

    private fun serializeQueuedSong(it: QueuedSong) = JSONObject().apply {
        put("url", it.track.info.uri)
        put("title", it.track.info.title)
        put("duration", it.track.duration / 1000)
        put("queued_by", it.queuedBy.name)
    }

    private fun serializeQueuedSong(it: AudioTrack) = JSONObject().apply {
        put("url", it.info.uri)
        put("title", it.info.title)
        put("duration", it.duration / 1000)
    }

    companion object {
        val musicSettings = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build(
                object : CacheLoader<String, MusicSettings?>() {
                    override fun load(p0: String): MusicSettings? {
                        val guid = Bot.getGuild(p0) ?: return null
                        return MusicSettings.get(guid).execute()
                    }
                }
        )

        fun parseMS(ms: Long): String = buildString {
            val totalSeconds = ms / 1000
            val totalMinutes = totalSeconds / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes - (hours * 60)
            val seconds = totalSeconds - ((hours * 3600) + (minutes * 60))
            if (hours > 0) {
                if (hours < 10)
                    append("0")
                append("$hours:")
            }
            if (minutes < 10)
                append("0")
            append("$minutes:")
            if (seconds < 10)
                append("0")
            append("$seconds")
        }
    }


    data class QueuedSong(val track: AudioTrack, val queuedBy: User, val queuedIn: String)

    fun resetQueue() {
        RedisConnector.get().use {
            it.del("music.queue:${this.guild.id}")
            it.del("music.playing:${this.guild.id}")
        }
    }

}