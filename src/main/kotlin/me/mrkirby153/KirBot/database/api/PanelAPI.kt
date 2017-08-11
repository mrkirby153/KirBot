package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject

object PanelAPI {

    internal val API_PROCESSOR: ApiProcessor = ApiProcessor(Bot.properties.getProperty("api-endpoint"), Bot.properties.getProperty("api-key"), true)

    private val apiProcessorThread: Thread = Thread(API_PROCESSOR).apply {
        name = "ApiProcessor"
        isDaemon = true
        start()
    }

    fun getRealname(user: User): ApiRequest<Realname> = object : ApiRequest<Realname>("/user/${user.id}/name") {
        override fun parse(json: JSONObject): Realname {
            return Realname(json.getString("first_name"), json.getString("last_name"))
        }
    }

    fun getRealnames(users: List<User>): ApiRequest<Realnames> {
        return object : ApiRequest<Realnames>("/user/names", Methods.POST, mutableMapOf(Pair("names",
                users.map { it.id }.joinToString(",")))) {
            override fun parse(json: JSONObject): Realnames {
                val map = mutableMapOf<User, Realname>()

                users.forEach {
                    val obj = json.optJSONObject(it.id) ?: return@forEach
                    map[it] = Realname(obj.getString("first_name"), obj.getString("last_name"))
                }
                return Realnames(map)
            }
        }
    }

    fun getCommands(guild: Guild): ApiRequest<GuildCommands> {
        return object : ApiRequest<GuildCommands>("/server/${guild.id}/commands") {
            override fun parse(json: JSONObject): GuildCommands {
                val cmds = mutableListOf<GuildCommand>()

                val array = json.getJSONArray("cmds")
                array.forEach { obj ->
                    val jsonObj = obj as JSONObject
                    cmds.add(GuildCommand(jsonObj.getString("name"),
                            jsonObj.getString("data"), Clearance.valueOf(jsonObj.getString("clearance")), jsonObj.getInt("respect_whitelist") == 1))
                }
                return GuildCommands(cmds)
            }
        }
    }

    fun guildSettings(guild: Guild): ApiRequest<GuildSettings> {
        return object : ApiRequest<GuildSettings>("/server/${guild.id}/settings") {
            override fun parse(json: JSONObject): GuildSettings {
                return GuildSettings(json.getString("name"), json.getString("realname"), json.getInt("require_realname") == 1,
                        json.getString("command_discriminator"), json.optString("log_channel"), json.getString("cmd_whitelist").split(","))
            }
        }
    }

    fun registerServer(guild: Guild): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/server/register", Methods.PUT, mapOf(Pair("name", guild.name), Pair("id", guild.id))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    fun unregisterServer(guild: Guild): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/server/${guild.id}", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    fun setServerName(guild: Guild): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/server/{$guild.id}/name", Methods.PATCH, mapOf(Pair("name", guild.name))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    fun registerChannel(channel: Channel): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/server/${channel.guild.id}/channel",
                Methods.PUT, mapOf(Pair("id", channel.id), Pair("name", channel.name), Pair("type", if (channel is TextChannel) "TEXT" else "Voice"))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    fun unregisterChannel(channel: Channel): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/channel/${channel.id}", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    fun updateChannel(channel: Channel): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/channel/${channel.id}", Methods.PATCH, mapOf(Pair("name", channel.name))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    fun getChannels(guild: Guild): ApiRequest<GuildChannels> {
        return object : ApiRequest<GuildChannels>("/server/${guild.id}/channels") {
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
    }
}