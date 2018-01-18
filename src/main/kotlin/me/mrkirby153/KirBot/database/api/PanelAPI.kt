package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

object PanelAPI {

    internal val API_ENDPOINT = Bot.properties.getProperty("api-endpoint")
    internal val API_KEY = Bot.properties.getProperty("api-key")

    internal val executor = Executors.newFixedThreadPool(3)

    fun unregisterServer(guild: Guild): ApiRequest<Void> {
        return object : ApiRequest<Void>("/server/${guild.id}", Methods.DELETE) {}
    }

    fun getChannels(guild: Guild) = object :
            ApiRequest<GuildChannels>("/server/${guild.id}/channels") {
        override fun parse(json: JSONObject): GuildChannels {
            val textChannels = mutableListOf<GuildChannel>()
            val voiceChannels = mutableListOf<GuildChannel>()

            json.getJSONArray("voice").forEach { obj ->
                val jsonObj = obj as JSONObject
                voiceChannels.add(GuildChannel(jsonObj.getString("id"), jsonObj.getString("server"),
                        jsonObj.getString("channel_name"), ChannelType.VOICE))
            }

            json.getJSONArray("text").forEach { obj ->
                val jsonObj = obj as JSONObject
                textChannels.add(GuildChannel(jsonObj.getString("id"), jsonObj.getString("server"),
                        jsonObj.getString("channel_name"), ChannelType.TEXT))
            }
            return GuildChannels(textChannels, voiceChannels)
        }
    }

    fun updateChannels(guild: Guild, callback: (() -> Unit)? = null) {
        Bot.LOG.debug("Updating channels on ${guild.name} (${guild.id})")
        getChannels(guild).queue { (text, voice) ->
            val channels = mutableListOf<String>()
            val toRegister = mutableListOf<String>()
            val toUnregister = mutableListOf<String>()

            voice.map { it.id }.toCollection(channels)
            text.map { it.id }.toCollection(channels)

            toUnregister.addAll(channels)
            toUnregister.removeAll(guild.textChannels.map { it.id })
            toUnregister.removeAll(guild.voiceChannels.map { it.id })

            toRegister.addAll(guild.textChannels.filter { it.id !in channels }.map { it.id })
            toRegister.addAll(guild.voiceChannels.filter { it.id !in channels }.map { it.id })

            Bot.LOG.debug("Registering channels $toRegister")
            Bot.LOG.debug("Unregistering channels $toUnregister")

            toRegister.forEach {
                val c = guild.getTextChannelById(it) as? Channel ?: guild.getVoiceChannelById(
                        it) as? Channel ?: return@forEach
                GuildChannel.register(c).queue()
            }

            toUnregister.forEach {
                GuildChannel.unregister(it).queue()
            }

            callback?.invoke()
        }
    }

    fun getGroups(guild: Guild): ApiRequest<List<Group>> {
        return object : ApiRequest<List<Group>>("/server/${guild.id}/groups") {
            override fun parse(json: JSONArray): List<Group> {
                val allGroups = mutableListOf<Group>()
                json.map { it as JSONObject }.forEach { g ->
                    val name = g.getString("group_name")
                    val role = g.getString("role_id")
                    val id = g.getString("id")
                    val gu = g.getString("server_id")
                    val members = mutableListOf<String>()

                    g.getJSONArray("members").map { it as JSONObject }.forEach { m ->
                        members.add(m.getString("user_id"))
                    }
                    val group = Group(id, gu, name, role, members)
                    allGroups.add(group)
                }
                return allGroups
            }
        }
    }

    fun getMembers(guild: Guild): ApiRequest<List<GuildMember>> {
        return object : ApiRequest<List<GuildMember>>("/server/${guild.id}/members") {

            override fun parse(json: JSONArray): List<GuildMember> {
                val list = mutableListOf<GuildMember>()
                json.map { it as JSONObject }.forEach {
                    list.add(GuildMember.parse(it))
                }
                return list
            }
        }

    }
}