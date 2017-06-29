package me.mrkirby153.KirBot.web

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameUpdater
import me.mrkirby153.KirBot.utils.hide
import me.mrkirby153.KirBot.utils.shard
import me.mrkirby153.KirBot.utils.unhide
import net.dv8tion.jda.core.Permission
import ro.pippo.controller.*
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
        guild.textChannels.mapTo(channels) {
            Channel(it.id, it.name, "TEXT", it.getPermissionOverride(guild.publicRole)?.denied?.contains(Permission.MESSAGE_READ) ?: false)
        }
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
        guild.textChannels.mapTo(channels) {
            Channel(it.id, it.name, "TEXT", it.getPermissionOverride(guild.publicRole)?.denied?.contains(Permission.MESSAGE_READ) ?: false)
        }
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

    @POST("/server/{id: [0-9]+}/channel/{channel: [0-9]+}/visibility")
    @Produces(Produces.JSON)
    fun hideChannel(@Param("id") id: String, @Param("channel") channel: String, @Param("visible") visible: Boolean): WebApp.Response {
        val guild = Bot.getGuild(id) ?: return WebApp.Response(false, "Guild not found!")
        val chan = guild.getTextChannelById(channel) ?: return WebApp.Response(false, "Channel not found!")
        if (visible) {
            chan.unhide()
            return WebApp.Response(true, "Channel $channel shown")
        } else {
            chan.hide()
            return WebApp.Response(true, "Channel $channel hidden")
        }
    }

    @POST("/server/{id: [0-9]+}/channel/{channel: [0-9]+}/grantAccess")
    @Produces(Produces.JSON)
    fun grantAccess(@Param("id") id: String, @Param("channel") channel: String, @Param("user") user: String): WebApp.Response {
        val guild = Bot.getGuild(id) ?: return WebApp.Response(false, "Guild not found!")
        val chan = guild.getTextChannelById(channel) ?: return WebApp.Response(false, "Channel not found!")

        val us = Bot.getUser(user) ?: return WebApp.Response(false, "User not found!")
        val member = guild.getMember(us)
        val override = chan.getPermissionOverride(member) ?: chan.createPermissionOverride(member).complete()
        override.manager.grant(Permission.MESSAGE_READ).queue()
        return WebApp.Response(true, "Given access to $user")
    }


    data class Channel(val id: String, val channel_name: String, val type: String, val private: Boolean = false)

    data class MusicQueue(val length: Int, val nowPlaying: QueuedSong?, val playing: Boolean, val songs: Array<QueuedSong>?)

    data class QueuedSong(val url: String, val title: String, val duration: Long)
}