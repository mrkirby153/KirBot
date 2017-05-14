package me.mrkirby153.KirBot.web

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameUpdater
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
    fun voice(@Param("id") id: String): Array<Channel>{
        val guild = Bot.getGuild(id) ?: return arrayOf(Channel("-1", "GUILD_NOT_FOUND", "NONE"))
        val channels = mutableListOf<Channel>()
        guild.voiceChannels.mapTo(channels) { Channel(it.id, it.name, "VOICE") }
        return channels.toTypedArray()
    }

    @GET("/channels/{id: [0-9]+}/text")
    @Produces(Produces.JSON)
    fun text(@Param("id") id: String): Array<Channel>{
        val guild = Bot.getGuild(id) ?: return arrayOf(Channel("-1", "GUILD_NOT_FOUND", "NONE"))
        val channels = mutableListOf<Channel>()
        guild.textChannels.mapTo(channels) { Channel(it.id, it.name, "TEXT") }
        return channels.toTypedArray()
    }


    data class Channel(val id: String, val channel_name: String, val type: String)
}