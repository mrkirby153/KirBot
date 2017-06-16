package me.mrkirby153.KirBot.web

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameUpdater
import me.mrkirby153.KirBot.utils.shard
import ro.pippo.controller.Controller
import ro.pippo.controller.GET
import ro.pippo.controller.Path
import ro.pippo.controller.Produces
import ro.pippo.controller.extractor.Param

@Path("/v1")
class ApiController : Controller() {

    @GET("/name/update")
    @Produces(Produces.JSON)
    fun updateName(): WebApp.Response {
        RealnameUpdater().run()
        return WebApp.Response(true, "Names updated!")
    }

    @GET("/channels/{id: [0-9]+}")
    @Produces(Produces.JSON)
    fun channels(@Param("id") id: String): Array<Channel> {
        val guild = Bot.getGuild(id) ?: return arrayOf(Channel("-1", "GUILD_NOT_FOUND", "NONE"))
        val channels = mutableListOf<Channel>()
        guild.voiceChannels.mapTo(channels) { Channel(it.id, it.name, "VOICE") }
        guild.textChannels.mapTo(channels) { Channel(it.id, it.name, "TEXT") }
        return channels.toTypedArray()
    }

    @GET("/channels/{id: [0-9]+}/voice")
    @Produces(Produces.JSON)
    fun voice(@Param("id") id: String): Array<Channel> {
        val guild = Bot.getGuild(id) ?: return arrayOf(Channel("-1", "GUILD_NOT_FOUND", "NONE"))
        val channels = mutableListOf<Channel>()
        guild.voiceChannels.mapTo(channels) { Channel(it.id, it.name, "VOICE") }
        return channels.toTypedArray()
    }

    @GET("/channels/{id: [0-9]+}/text")
    @Produces(Produces.JSON)
    fun text(@Param("id") id: String): Array<Channel> {
        val guild = Bot.getGuild(id) ?: return arrayOf(Channel("-1", "GUILD_NOT_FOUND", "NONE"))
        val channels = mutableListOf<Channel>()
        guild.textChannels.mapTo(channels) { Channel(it.id, it.name, "TEXT") }
        return channels.toTypedArray()
    }

    @GET("/server/{id: [0-9]+}/queue")
    @Produces(Produces.JSON)
    fun musicQueue(@Param("id") id: String): MusicQueue? {
        val queue = mutableListOf<QueuedSong>()
        val serverData = Bot.getGuild(id)?.shard()?.getServerData(id.toLong()) ?: return null
        serverData.musicManager.trackScheduler.queue.forEach {
            queue.add(QueuedSong(it.info.uri, it.info.title, it.duration / 1000))
        }
        val nowPlaying = serverData.musicManager.trackScheduler.nowPlaying
        val nowPlayingQueued = if (nowPlaying != null) QueuedSong(nowPlaying.info.uri, nowPlaying.info.title, nowPlaying.duration / 1000) else null
        return MusicQueue(serverData.musicManager.trackScheduler.queueLength(),
                nowPlayingQueued, serverData.musicManager.trackScheduler.playing, queue.toTypedArray())
    }


    data class Channel(val id: String, val channel_name: String, val type: String)

    data class MusicQueue(val length: Int, val nowPlaying: QueuedSong?, val playing: Boolean, val songs: Array<QueuedSong>?)

    data class QueuedSong(val url: String, val title: String, val duration: Long)
}