package me.mrkirby153.KirBot.music

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.MusicSettings
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import java.util.*
import java.util.concurrent.TimeUnit

class MusicManager(val guild: Guild) {

    val audioPlayer = Bot.playerManager.createPlayer()

    private val sender = AudioPlayerSendHandler(audioPlayer)

    val queue = LinkedList<QueuedSong>()

    val nowPlaying: AudioTrack?
        get() = audioPlayer.playingTrack

    val playing: Boolean
        get() = !audioPlayer.isPaused

    val trackScheduler = TrackScheduler(this)

    init {
        guild.audioManager.sendingHandler = sender
        audioPlayer.addListener(trackScheduler)
    }

    fun disconnect() {
        guild.audioManager.closeAudioConnection()
    }

    fun queueLength(): Long {
        var length = 0L
        queue.forEach {
            length += it.track.duration
        }
        return length
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
            if(hours > 0) {
                if (hours < 10)
                    append("0")
                append("$hours:")
            }
            if(minutes < 10)
                append("0")
            append("$minutes:")
            if(seconds < 10)
                append("0")
            append("$seconds")
        }
    }


    data class QueuedSong(val track: AudioTrack, val queuedBy: User, val queuedIn: String)

}