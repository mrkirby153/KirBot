package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import org.json.JSONObject
import java.util.concurrent.Executors

object PanelAPI {

    internal val API_ENDPOINT = Bot.properties.getProperty("api-endpoint")
    internal val API_KEY = Bot.properties.getProperty("api-key")

    internal val executor = Executors.newFixedThreadPool(3)

    fun getRealname(user: User): ApiRequest<Realname> = object : ApiRequest<Realname>("/user/${user.id}/name") {
        override fun parse(json: JSONObject): Realname {
            return Realname(json.getString("first_name"), json.getString("last_name"))
        }
    }

    fun getRealnames(users: List<User>): ApiRequest<Map<User, Realname?>> {
        return object : ApiRequest<Map<User, Realname?>>("/user/names", Methods.POST, mutableMapOf(Pair("names",
                users.map { it.id }.joinToString(",")))) {
            override fun parse(json: JSONObject): Map<User, Realname?> {
                val map = mutableMapOf<User, Realname?>()

                users.forEach {
                    val obj = json.optJSONObject(it.id)
                    if (obj == null)
                        map[it] = null
                    else
                        map[it] = Realname(obj.getString("first_name"), obj.getString("last_name"))
                }
                return map
            }
        }
    }

    fun getCommands(guild: Guild): ApiRequest<List<GuildCommand>> {
        return object : ApiRequest<List<GuildCommand>>("/server/${guild.id}/commands") {
            override fun parse(json: JSONObject): List<GuildCommand> {
                val cmds = mutableListOf<GuildCommand>()

                val array = json.getJSONArray("cmds")
                array.forEach { obj ->
                    val jsonObj = obj as JSONObject
                    cmds.add(GuildCommand(jsonObj.getString("name"),
                            jsonObj.getString("data"), Clearance.valueOf(jsonObj.getString("clearance")), jsonObj.getInt("respect_whitelist") == 1))
                }
                return cmds
            }
        }
    }

    fun guildSettings(guild: Guild): ApiRequest<GuildSettings> {
        return object : ApiRequest<GuildSettings>("/server/${guild.id}/settings") {
            override fun parse(json: JSONObject): GuildSettings {
                val whitelist = json.getString("cmd_whitelist")
                val managementRoles = json.getJSONArray("bot_manager")
                val roles = mutableListOf<String>()
                managementRoles.forEach { roles.add(it.toString()) }
                return GuildSettings(json.getString("name"), RealnameSetting.valueOf(json.getString("realname")), json.getInt("require_realname") == 1,
                        json.getString("command_discriminator"), json.optString("log_channel"), if (whitelist.isNotEmpty()) whitelist.split(",") else listOf(), roles)
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
        return object : ApiRequest<VoidApiResponse>("/server/${guild.id}/name", Methods.POST, mapOf(Pair("name", guild.name))) {
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

    fun unregisterChannel(channel: String): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/channel/$channel", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    fun updateChannel(channel: Channel): ApiRequest<VoidApiResponse> {
        val isHidden = channel.getPermissionOverride(channel.guild.publicRole)?.denied?.contains(Permission.MESSAGE_READ) ?: false
        return object : ApiRequest<VoidApiResponse>("/channel/${channel.id}", Methods.PATCH, mapOf(Pair("name", channel.name), Pair("hidden", isHidden.toString()))) {
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
                val c = guild.getTextChannelById(it) as? Channel ?: guild.getVoiceChannelById(it) as? Channel ?: return@forEach
                registerChannel(c).queue()
            }

            toUnregister.forEach {
                unregisterChannel(it).queue()
            }

            callback?.invoke()
        }
    }

    fun getMusicSettings(guid: Guild): ApiRequest<MusicSettings> {
        return object : ApiRequest<MusicSettings>("/server/${guid.id}/music") {
            override fun parse(json: JSONObject): MusicSettings {
                val whitelist = if (json.getString("mode").equals("off", true)) "" else json.getString("channels")
                val blacklistedSongs = if (json.getString("blacklist_songs").isEmpty()) arrayListOf<String>() else json.getString("blacklisted_songs").split(",")

                return MusicSettings(json.getInt("enabled") == 1, json.getString("mode"),
                        if (whitelist.isEmpty()) arrayOf<String>() else whitelist.split(",").toTypedArray(),
                        blacklistedSongs.toTypedArray(), json.getInt("max_queue_length"),
                        json.getInt("playlists") == 1, json.getInt("max_song_length"),
                        json.getInt("skip_cooldown"), json.getInt("skip_timer"))
            }
        }
    }

    fun logMessage(message: Message): ApiRequest<ServerMessage> {
        return object : ApiRequest<ServerMessage>("/message", Methods.PUT,
                mapOf(Pair("server", message.guild.id), Pair("id", message.id), Pair("channel", message.channel.id),
                        Pair("author", message.author.id), Pair("message", if (message.content.isNotEmpty()) message.content else " "))) {
            override fun parse(json: JSONObject): ServerMessage {
                return ServerMessage(json.getString("id"), json.getString("channel"),
                        json.getString("server_id"), json.getString("author"), json.getString("message"))
            }
        }
    }

    fun getMessage(id: String): ApiRequest<ServerMessage> {
        return object : ApiRequest<ServerMessage>("/message/$id") {
            override fun parse(json: JSONObject): ServerMessage {
                return ServerMessage(json.getString("id"), json.getString("channel"),
                        json.getString("server_id"), json.getString("author"), json.getString("message"))
            }
        }
    }

    fun deleteMessage(id: String): ApiRequest<ServerMessage> {
        return object : ApiRequest<ServerMessage>("/message/$id", Methods.DELETE) {
            override fun parse(json: JSONObject): ServerMessage {
                if (json.keySet().size < 1)
                    return ServerMessage("-1", "-1", "-1", "-1", "-1")
                else
                    return ServerMessage(json.getString("id"), json.getString("channel"),
                            json.getString("server_id"), json.getString("author"), json.getString("message"))
            }
        }
    }

    fun editMessage(message: Message): ApiRequest<ServerMessage> {
        return object : ApiRequest<ServerMessage>("/message/${message.id}", Methods.PATCH, mapOf(Pair("message", message.content))) {
            override fun parse(json: JSONObject): ServerMessage {
                if (json.keySet().size < 1)
                    return ServerMessage("-1", "-1", "-1", "-1", "-1")
                else
                    return ServerMessage(json.getString("id"), json.getString("channel"),
                            json.getString("server_id"), json.getString("author"), json.getString("message"))
            }

        }
    }

    fun bulkDelete(messages: Collection<String>): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/message/bulkDelete", Methods.DELETE, mapOf(Pair("messages", messages.joinToString(",")))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }

        }
    }

    fun createRole(role: net.dv8tion.jda.core.entities.Role): ApiRequest<GuildRole> {
        return object : ApiRequest<GuildRole>("/role", Methods.POST, mapOf(Pair("id", role.id),
                Pair("server_id", role.guild!!.id), Pair("name", role.name), Pair("permissions", role.permissionsRaw.toString()))) {
            override fun parse(json: JSONObject): GuildRole {
                return GuildRole(json.getString("id"), json.getString("name"), json.getString("server_id"), json.getLong("permissions"))
            }
        }
    }

    fun deleteRole(id: String): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/role/$id", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }

        }
    }

    fun updateRole(role: net.dv8tion.jda.core.entities.Role): ApiRequest<GuildRole> {
        return object : ApiRequest<GuildRole>("/role/${role.id}", Methods.PATCH, mapOf(Pair("name", role.name), Pair("permissions", role.permissionsRaw.toString()))) {
            override fun parse(json: JSONObject): GuildRole {
                return GuildRole(json.getString("id"), json.getString("name"), json.getString("server_id"), json.getLong("permissions"))
            }
        }
    }

    fun getRole(role: net.dv8tion.jda.core.entities.Role): ApiRequest<GuildRole> {
        return object : ApiRequest<GuildRole>("/role/${role.id}", Methods.GET) {
            override fun parse(json: JSONObject): GuildRole {
                return GuildRole(json.getString("id"), json.getString("name"), json.getString("server_id"),
                        json.getLong("permissions"))
            }
        }
    }

    fun getRoles(guild: Guild): ApiRequest<List<GuildRole>> {
        return object : ApiRequest<List<GuildRole>>("/server/${guild.id}/roles") {
            override fun parse(json: JSONObject): List<GuildRole> {
                val roles = mutableListOf<GuildRole>()
                json.getJSONArray("roles").forEach { r ->
                    val j = r as JSONObject
                    roles.add(GuildRole(j.getString("id"), j.getString("name"), j.getString("server_id"), j.getLong("permissions")))
                }
                return roles
            }
        }
    }

    fun quote(messageById: Message): ApiRequest<Quote> {
        return object : ApiRequest<Quote>("/server/quote",
                Methods.PUT, mapOf(Pair("server_id", messageById.guild.id), Pair("user", messageById.author.name), Pair("content", messageById.content), Pair("message_id", messageById.id))) {
            override fun parse(json: JSONObject): Quote {
                return Quote(json.getInt("id"), json.getString("message_id"), json.getString("user"), json.getString("server_id"), json.getString("content"))
            }

        }
    }

    fun getQuote(id: String): ApiRequest<Quote?> {
        return object : ApiRequest<Quote?>("/server/quote/$id") {
            override fun parse(json: JSONObject): Quote? {
                if (!json.has("id")) {
                    return null
                }
                return Quote(json.getInt("id"), json.getString("message_id"), json.getString("user"), json.getString("server_id"), json.getString("content"))
            }
        }
    }

    fun getQuotesForGuild(guild: Guild): ApiRequest<Array<Quote>> {
        return object : ApiRequest<Array<Quote>>("/server/${guild.id}/quotes") {
            override fun parse(json: JSONObject): Array<Quote> {
                val quotes = mutableListOf<Quote>()
                json.getJSONArray("quotes").forEach { q ->
                    if(q is JSONObject){
                        quotes.add(Quote(q.getInt("id"), q.getString("message_id"), q.getString("user"),
                                q.getString("server_id"), q.getString("content")))
                    }
                }
                return quotes.toTypedArray()
            }
        }
    }

    fun serverExists(guild: Guild): ApiRequest<Boolean> {
        return object : ApiRequest<Boolean>("/server/${guild.id}") {
            override fun parse(json: JSONObject): Boolean {
                return json.getBoolean("exists")
            }

        }
    }

    fun getGroups(guild: Guild): ApiRequest<List<Group>>{
        return object : ApiRequest<List<Group>>("/server/${guild.id}/groups") {
            override fun parse(json: JSONObject): List<Group> {
                val allGroups = mutableListOf<Group>()
                json.getJSONArray("groups").map { it as JSONObject }.forEach { g ->
                    val name = g.getString("group_name")
                    val role = g.getString("role_id")
                    val id = g.getString("id")
                    val gu = g.getString("server_id")
                    val members = mutableListOf<String>()

                    g.getJSONArray("members").map { it as JSONObject }.forEach {  m ->
                        members.add(m.getString("user_id"))
                    }
                    val group = Group(id, gu, name, role, members)
                    allGroups.add(group)
                }
                return allGroups
            }
        }
    }

    fun createGroup(guild: Guild, name: String, role: Role): ApiRequest<Group> {
        return object : ApiRequest<Group>("/server/${guild.id}/groups", Methods.PUT, mapOf(Pair("name", name), Pair("role", role.id))) {
            override fun parse(json: JSONObject): Group {
                return Group(json.getString("id"), json.getString("server_id"), json.getString("group_name"), json.getString("role_id"), mutableListOf())
            }
        }
    }
}