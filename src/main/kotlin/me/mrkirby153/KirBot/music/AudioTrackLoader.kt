package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.Time
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.mdEscape
import net.dv8tion.jda.core.entities.User

class AudioTrackLoader(val manager: MusicManager, val requestedBy: User, val context: Context,
                       val queuePosition: Int = 0, val callback: ((AudioTrack) -> Unit)? = null) :
        AudioLoadResultHandler {
    override fun loadFailed(p0: FriendlyException?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun trackLoaded(p0: AudioTrack) {
        val queuedSong = MusicManager.QueuedSong(p0, requestedBy, context.channel.id)
        var startIndex: Int
        if (queuePosition != -1) {
            startIndex = if (queuePosition < 1) 0 else queuePosition - 1
            manager.queue.add(startIndex, queuedSong)
        } else {
            startIndex = manager.queue.size
            manager.queue.addLast(queuedSong)
        }
        val modifiedQueue = mutableListOf<AudioTrack>()
        if (manager.nowPlaying != null)
            modifiedQueue.add(manager.nowPlaying!!)
        modifiedQueue.addAll(manager.queue.map { it.track })
        var toSubtract = 0L
        val queueLength = modifiedQueue.subList(0, Math.min(modifiedQueue.size - 1, startIndex + 1)).sumBy {
            println("Song ${it.info.title} :: ${it.position}")
            toSubtract += it.position
            it.duration.toInt()
        } - toSubtract
        val position = manager.queue.indexOf(queuedSong)
        // Check track duration
        val settings = MusicManager.musicSettings[context.guild.id] ?: return
        if (settings.maxSongLength != -1)
            if (p0.duration / (60 * 1000) > settings.maxSongLength) {
                context.send().error("That song is too long. The max song length is ${Time.format(1,
                        (settings.maxSongLength * 60 * 1000).toLong(), Time.TimeUnit.FIT)}").queue()
                return
            }
        // If the bot isn't playing, start
        if (!context.guild.selfMember.voiceState.inVoiceChannel()) {
            manager.audioPlayer.volume = 100
            context.guild.audioManager.openAudioConnection(
                    requestedBy.getMember(context.guild).voiceState.channel)
            manager.trackScheduler.playNextTrack()
        }
        manager.audioPlayer.isPaused = false
        if (manager.nowPlaying == null) {
            manager.trackScheduler.playNextTrack()
        }
        callback?.invoke(p0)
        if (manager.queue.size == 1 && manager.nowPlaying == null) {
            return
        }
        context.send().embed("Added to Queue") {
            if (p0.info.uri.contains("youtu")) {
                thumbnail = "https://i.ytimg.com/vi/${p0.info.identifier}/default.jpg"
            }
            description { +("**${p0.info.title.mdEscape()}**" link p0.info.uri) }

            fields {
                field {
                    title = "Song Duration"
                    inline = true
                    description = MusicManager.parseMS(p0.duration)
                }
                field {
                    title = "Author"
                    inline = true
                    description = p0.info.author
                }

                val playing = if (queueLength < 1000) "NOW!" else MusicManager.parseMS(
                        queueLength)
                field {
                    title = "Estimated Time until playing"
                    inline = true
                    description = "~$playing".mdEscape()
                }
                field {
                    title = "Position in queue"
                    inline = true
                    description = (position + 1).toString()
                }
            }
        }.rest().queue()
        manager.updateQueue()
    }

    override fun noMatches() {
        context.send().error("No matches were found for that song. Please try again").queue()
    }

    override fun playlistLoaded(p0: AudioPlaylist) {
        val settings = MusicManager.musicSettings[context.guild.id] ?: return
        if (!settings.playlists) {
            context.send().error("Playlists cannot currently be played").queue()
            return
        }
        var duration = 0L
        p0.tracks.forEach {
            duration += it.duration
        }
        if (settings.maxQueueLength != -1 && (manager.queueLength() + duration) / (60 * 1000) > settings.maxQueueLength) {
            context.send().error(
                    "Queueing this playlist will make the queue too long, try again when the queue is shorter").queue()
            return
        }
        val failedTracks = mutableListOf<AudioTrack>()
        p0.tracks.forEach {
            if (settings.maxSongLength != -1) {
                if (it.duration / (60 * 1000) > settings.maxSongLength) {
                    failedTracks.add(it)
                    return
                }
            }
            manager.queue.addLast(MusicManager.QueuedSong(it, requestedBy, context.channel.id))
        }
        p0.tracks.filter { it !in failedTracks }.forEach {
            duration += it.duration
        }
        context.send().embed("Added to Queue") {
            description {
                append("**${p0.name}**")

                append("\n\n Queued `${p0.tracks.size - failedTracks.size}` songs totaling `${MusicManager.parseMS(
                        duration)}`")

                if (failedTracks.isNotEmpty()) {
                    append("\n\nFailed to queue `${failedTracks.size}` songs because they were too long.\n\n")
                    failedTracks.forEach {
                        append("\t${it.info.title}" link it.info.uri)
                        append("\n")
                        if (length > 1500)
                            return@forEach
                    }
                }
            }
        }.rest().queue()
        // If the bot isn't playing, start
        if (!context.guild.selfMember.voiceState.inVoiceChannel()) {
            manager.audioPlayer.volume = 100
            context.guild.audioManager.openAudioConnection(
                    requestedBy.getMember(context.guild).voiceState.channel)
            manager.trackScheduler.playNextTrack()
        }
        manager.audioPlayer.isPaused = false
        if (manager.nowPlaying == null) {
            manager.trackScheduler.playNextTrack()
        }
    }
}