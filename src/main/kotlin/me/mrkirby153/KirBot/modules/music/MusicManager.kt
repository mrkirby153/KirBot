package me.mrkirby153.KirBot.modules.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.VoiceChannel
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList
import java.util.concurrent.TimeUnit

class MusicManager(val guild: Guild) {

    val audioPlayer = Bot.playerManager.createPlayer()

    private val sender = AudioPlayerSendHandler(audioPlayer)

    val queue = LinkedList<QueuedSong>()

    val nowPlaying: AudioTrack?
        get() = audioPlayer.playingTrack

    val playing: Boolean
        get() = !audioPlayer.isPaused

    var boundChannelId: String? = null

    val boundChannel: TextChannel?
        get() = guild.getTextChannelById(boundChannelId)

    var connected = false

    var manualPaused = false

    init {
        guild.audioManager.sendingHandler = sender
        audioPlayer.addListener(TrackScheduler(this))
    }

    /**
     * Disconnects the bot from the current voice channel and resets everything
     */
    fun disconnect() {
        connected = false
        guild.audioManager.closeAudioConnection()
        audioPlayer.playTrack(null)
        boundChannelId = null
        queue.clear()
        ModuleManager[MusicModule::class.java].stopPlaying(this.guild)
        ModuleManager[Redis::class.java].getConnection().use {
            it.del("music.queue:${this.guild.id}")
            it.del("music.playing:${this.guild.id}")
            it.del(*it.keys("music:${guild.id}:channel:*").toTypedArray())
        }
    }

    /**
     * Plays the next track
     */
    fun playNextTrack() {
        if (queue.isNotEmpty()) {
            val track = queue.removeFirst()
            Bot.LOG.debug("[MUSIC/${guild.id}] Playing ${track.track.info.title}")
            audioPlayer.playTrack(track.track)
        } else {
            Bot.scheduler.schedule({
                disconnect()
            }, 100, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * Connects to the voice channel
     *
     * @param channel The voice channel to connect to
     * @param channelToBind The channel to bind to
     */
    fun connect(channel: VoiceChannel, channelToBind: TextChannel? = null) {
        this.guild.audioManager.openAudioConnection(channel)
        connected = true
        ModuleManager[MusicModule::class.java].startPlaying(this.guild)
    }

    /**
     * Sets the bound text channel
     *
     * @param channel The channel to bind to
     */
    fun bind(channel: TextChannel) {
        this.boundChannelId = channel.id
    }

    /**
     * Queues a track to be played
     *
     * @param track The track to queue
     * @param requester The user who requested the track
     */
    fun queue(track: AudioTrack, requester: User) {
        this.queue.add(QueuedSong(track, requester))
    }

    /**
     * Pause the currently playing song
     *
     * @param auto If the song was automatically paused
     */
    fun pause(auto: Boolean = false) {
        Bot.LOG.debug("[MUSIC/${guild.id}] Pausing music ($auto)")
        if (!auto)
            manualPaused = true
        audioPlayer.isPaused = true
    }

    /**
     * Resumes the song
     *
     * @param auto If the song was automatically resumed
     */
    fun resume(auto: Boolean = false) {
        Bot.LOG.debug("[MUSIC/${guild.id}] Resuming music ($auto)")
        if (auto) {
            if (manualPaused)
                return
        }
        audioPlayer.isPaused = false
        manualPaused = false
    }

    fun queueLength(): Int {
        return queue.sumBy { it.track.duration.toInt() }
    }

    fun updateQueue() {
        if (playing && this.nowPlaying != null) {
            ModuleManager[Redis::class.java].getConnection().use {
                it.set("music.playing:${guild.id}",
                        serializeQueuedSong(this.nowPlaying!!, null).toString())
            }
        }
        ModuleManager[Redis::class.java].getConnection().use {
            val queue = this.queue.map { serializeQueuedSong(it.track, it.queuedBy) }
            val arr = JSONArray()
            queue.forEach { arr.put(it) }
            it.set("music.queue:${guild.id}", arr.toString())
        }
    }

    fun updateVoiceState() {
        ModuleManager[Redis::class.java].getConnection().use { redis ->
            val keys = redis.keys("music:${guild.id}:channel:*")
            this.guild.selfMember.voiceState.channel?.members?.forEach {
                redis.set("music:${guild.id}:channel:${it.user.id}", "true")
                keys.remove("music:${guild.id}:channel:${it.user.id}")
            }
            if (keys.isNotEmpty())
                redis.del(*keys.toTypedArray())
        }
    }

    fun serializeQueuedSong(it: AudioTrack, queuedBy: User? = null) = JSONObject().apply {
        put("url", it.info.uri)
        put("title", it.info.title)
        put("duration", it.duration / 1000)
        put("position", it.position / 1000)
        put("queued_by", queuedBy?.nameAndDiscrim ?: "Unknown")
    }


    data class QueuedSong(val track: AudioTrack, val queuedBy: User)

    companion object {
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
}