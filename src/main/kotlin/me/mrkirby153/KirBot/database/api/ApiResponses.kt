package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import org.json.JSONObject

class Realname(val firstName: String, val lastName: String) {
    companion object {
        fun get(user: User): ApiRequest<Realname> = object : ApiRequest<Realname>("/user/${user.id}/name") {
            override fun parse(json: JSONObject): Realname {
                return Realname(json.getString("first_name"), json.getString("last_name"))
            }
        }

        fun get(users: Collection<User>) = object : ApiRequest<Map<User, Realname?>>("/user/names", Methods.POST, mutableMapOf(Pair("names",
                users.joinToString(",") { it.id }))) {
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
}

class GuildCommand(val name: String, val data: String, val clearance: Clearance, val respectWhitelist: Boolean) {
    companion object {
        fun getCommands(guild: Guild) = object : ApiRequest<List<GuildCommand>>("/server/${guild.id}/commands") {
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
}

class GuildSettings(val name: String, val nick: String?, val realnameSetting: RealnameSetting, val requireRealname: Boolean,
                    val cmdDiscriminator: String, val logChannel: String?, val whitelistedChannels: List<String>,
                    val managerRoles: List<String>) {
    companion object {
        fun get(guild: Guild) = object : ApiRequest<GuildSettings>("/server/${guild.id}/settings") {
            override fun parse(json: JSONObject): GuildSettings {
                val managementRoles = json.getJSONArray("bot_manager")
                val roles = mutableListOf<String>()
                managementRoles.forEach { roles.add(it.toString()) }
                return GuildSettings(json.getString("name"), json.optString("bot_nick"),
                        RealnameSetting.valueOf(json.getString("realname")),
                        json.getInt("require_realname") == 1,
                        json.getString("command_discriminator"), json.optString("log_channel"),
                        json.getJSONArray("cmd_whitelist").map { it.toString() }, roles)
            }
        }
    }
}

enum class ChannelType {
    VOICE,
    TEXT
}

class GuildChannel(val id: String, val guild: String, val name: String, val type: ChannelType) {
    val channel: Channel = when (type) {
         ChannelType.TEXT -> Bot.getGuild(guild)!!.getTextChannelById(id)
         ChannelType.VOICE -> Bot.getGuild(guild)!!.getVoiceChannelById(id)
     }


    fun update(): ApiRequest<VoidApiResponse> {
        val isHidden = channel.getPermissionOverride(channel.guild.publicRole)?.denied?.contains(Permission.MESSAGE_READ) ?: false
        return object : ApiRequest<VoidApiResponse>("/channel/${channel.id}", Methods.PATCH,
                mapOf(Pair("name", channel.name), Pair("hidden", isHidden.toString()))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse();
            }
        }
    }

    companion object {
        fun register(channel: Channel): ApiRequest<VoidApiResponse> {
            return object : ApiRequest<VoidApiResponse>("/server/${channel.guild.id}/channel",
                    Methods.PUT, mapOf(Pair("id", channel.id), Pair("name", channel.name), Pair("type", if (channel is TextChannel) "TEXT" else "Voice"))) {
                override fun parse(json: JSONObject): VoidApiResponse {
                    return VoidApiResponse()
                }
            }
        }

        fun unregister(channel: String): ApiRequest<VoidApiResponse> {
            return object : ApiRequest<VoidApiResponse>("/channel/$channel", Methods.DELETE) {
                override fun parse(json: JSONObject): VoidApiResponse {
                    return VoidApiResponse()
                }
            }
        }
    }
}

data class GuildChannels(val text: List<GuildChannel>, val voice: List<GuildChannel>)

class MusicSettings(val enabled: Boolean, whitelist: String, val channels: Array<String>, val blacklistedSongs: Array<String>,
                    val maxQueueLength: Int, val playlists: Boolean, val maxSongLength: Int, val skipCooldown: Int, val skipTimer: Int) {

    val whitelistMode = WhitelistMode.valueOf(whitelist)

    companion object {
        fun get(guid: Guild) = object : ApiRequest<MusicSettings>("/server/${guid.id}/music") {
            override fun parse(json: JSONObject): MusicSettings {
                return MusicSettings(json.getInt("enabled") == 1, json.getString("mode"),
                        json.getJSONArray("channels").map { it.toString() }.toTypedArray(),
                        arrayOf(""), json.getInt("max_queue_length"),
                        json.getInt("playlists") == 1, json.getInt("max_song_length"),
                        json.getInt("skip_cooldown"), json.getInt("skip_timer"))
            }
        }
    }

    enum class WhitelistMode {
        OFF,
        BLACKLIST,
        WHITELIST
    }
}

class ServerMessage(val id: String?, val channelId: String, val serverId: String, val authorId: String, val content: String) {
    val guild = Bot.getGuild(this.serverId)
    val channel = guild?.getTextChannelById(this.channelId)
    val author = Bot.getUser(this.authorId)

    companion object {
        fun get(id: String): ApiRequest<ServerMessage> = object : ApiRequest<ServerMessage>("/message/$id") {
            override fun parse(json: JSONObject): ServerMessage {
                return ServerMessage(json.getString("id"), json.getString("channel"),
                        json.getString("server_id"), json.getString("author"), json.getString("message"))
            }
        }

        fun delete(id: String) = object : ApiRequest<ServerMessage>("/message/$id", Methods.DELETE) {
            override fun parse(json: JSONObject): ServerMessage {
                return if (json.keySet().size < 1)
                    ServerMessage("-1", "-1", "-1", "-1", "-1")
                else
                    ServerMessage(json.getString("id"), json.getString("channel"),
                            json.getString("server_id"), json.getString("author"), json.getString("message"))
            }
        }

        fun bulkDelete(messages: Collection<String>) = object : ApiRequest<VoidApiResponse>("/message/bulkDelete", Methods.DELETE,
                mapOf(Pair("messages", messages.joinToString(",")))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }

        }
    }
}

class GuildRole(val id: String, val name: String, val serverId: String, val permissions: Long) {
    val guild = Bot.getGuild(serverId)
    val role = guild?.getRoleById(id)

    fun delete() = object : ApiRequest<VoidApiResponse>("/role/$id", Methods.DELETE) {
        override fun parse(json: JSONObject): VoidApiResponse {
            return VoidApiResponse()
        }
    }

    fun update() = object : ApiRequest<GuildRole>("/role/${role!!.id}",
            Methods.PATCH, mapOf(Pair("name", role.name), Pair("permissions", role.permissionsRaw.toString()))) {
        override fun parse(json: JSONObject): GuildRole {
            return GuildRole(json.getString("id"), json.getString("name"), json.getString("server_id"),
                    json.getLong("permissions"))
        }
    }

    companion object {
        fun create(role: net.dv8tion.jda.core.entities.Role) = object : ApiRequest<GuildRole>("/role", Methods.POST,
                mapOf(Pair("id", role.id), Pair("server_id", role.guild!!.id), Pair("name", role.name), Pair("permissions", role.permissionsRaw.toString()))) {
            override fun parse(json: JSONObject): GuildRole {
                return GuildRole(json.getString("id"), json.getString("name"),
                        json.getString("server_id"), json.getLong("permissions"))
            }
        }

        fun get(role: net.dv8tion.jda.core.entities.Role) = object : ApiRequest<GuildRole>("/role/${role.id}", Methods.GET) {
            override fun parse(json: JSONObject): GuildRole {
                return GuildRole(json.getString("id"), json.getString("name"), json.getString("server_id"),
                        json.getLong("permissions"))
            }
        }
        fun delete(id: String) = object : ApiRequest<VoidApiResponse>("/role/$id", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }
}

class Quote(val id: Int, val messageId: String, val user: String, val server: String, val content: String) {
    val guild = Bot.getGuild(server)

    companion object {
        fun create(message: Message) = object : ApiRequest<Quote>("/server/quote",
                Methods.PUT, mapOf(Pair("server_id", message.guild.id), Pair("user", message.author.name), Pair("content", message.content), Pair("message_id", message.id))) {
            override fun parse(json: JSONObject): Quote {
                return Quote(json.getInt("id"), json.getString("message_id"), json.getString("user"), json.getString("server_id"), json.getString("content"))
            }
        }

        fun get(id: String) = object : ApiRequest<Quote?>("/server/quote/$id") {
            override fun parse(json: JSONObject): Quote? {
                if (!json.has("id")) {
                    return null
                }
                return Quote(json.getInt("id"), json.getString("message_id"), json.getString("user"),
                        json.getString("server_id"), json.getString("content"))
            }
        }

        fun get(guild: Guild): ApiRequest<Array<Quote>> = object : ApiRequest<Array<Quote>>("/server/${guild.id}/quotes") {
            override fun parse(json: JSONObject): Array<Quote> {
                val quotes = mutableListOf<Quote>()
                json.getJSONArray("quotes").forEach { q ->
                    if (q is JSONObject) {
                        quotes.add(Quote(q.getInt("id"), q.getString("message_id"), q.getString("user"),
                                q.getString("server_id"), q.getString("content")))
                    }
                }
                return quotes.toTypedArray()
            }
        }
    }
}

class Group(val id: String, val guild: String, val name: String, val roleId: String, val members: MutableList<String>) {

    val role: Role? = Bot.getGuild(guild)?.getRoleById(roleId)

    fun addUser(user: User): ApiRequest<VoidApiResponse>? {
        // Check if the user is in the group first
        if (user.id in members)
            return null

        return object : ApiRequest<VoidApiResponse>("/group/$id/member", Methods.PUT, mapOf(Pair("id", user.id))) {
            override fun parse(json: JSONObject): VoidApiResponse {
                members.add(user.id)
                val guild = Bot.getGuild(guild)
                if (guild != null) {
                    val role = guild.getRoleById(roleId)
                    if (role != null) {
                        guild.controller.addRolesToMember(user.getMember(guild), role).queue()
                    }
                }
                return VoidApiResponse()
            }
        }
    }

    fun removeUser(user: User): ApiRequest<VoidApiResponse>? {
        if (user.id !in members) {
            return null
        }

        return object : ApiRequest<VoidApiResponse>("/group/$id/member/${user.id}", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                members.remove(user.id)
                val guild = Bot.getGuild(guild)
                if (guild != null) {
                    val role = guild.getRoleById(roleId)
                    if (role != null) {
                        guild.controller.removeRolesFromMember(user.getMember(guild), role).queue()
                    }
                }
                return VoidApiResponse()
            }
        }
    }

    fun delete(): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/group/$id", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }

        }
    }

    companion object {
        fun create(guild: Guild, name: String, role: Role) = object : ApiRequest<Group>("/server/${guild.id}/groups", Methods.PUT, mapOf(Pair("name", name), Pair("role", role.id))) {
            override fun parse(json: JSONObject): Group {
                return Group(json.getString("id"), json.getString("server_id"), json.getString("group_name"), json.getString("role_id"), mutableListOf())
            }
        }

    }
}

class ClearanceOverride(val id: Int, val command: String, cl: String) {
    var clearance = Clearance.valueOf(cl)

    fun update(): ApiRequest<ClearanceOverride> {
        return object : ApiRequest<ClearanceOverride>("/overrides/$id", Methods.PATCH, mapOf(Pair("clearance", clearance.toString()))) {
            override fun parse(json: JSONObject): ClearanceOverride {
                return this@ClearanceOverride
            }
        }
    }

    fun delete(): ApiRequest<VoidApiResponse> {
        return object : ApiRequest<VoidApiResponse>("/overrides/$id", Methods.DELETE) {
            override fun parse(json: JSONObject): VoidApiResponse {
                return VoidApiResponse()
            }
        }
    }

    companion object {
        fun create(guild: Guild, command: String, clearance: Clearance) = object : ApiRequest<ClearanceOverride>("/server/${guild.id}/overrides", Methods.PUT, mapOf(Pair("command", command), Pair("clearance", clearance.toString()))) {
            override fun parse(json: JSONObject): ClearanceOverride {
                return ClearanceOverride(json.getInt("id"), json.getString("command"), json.getString("clearance"))
            }

        }

        fun get(guild: Guild): ApiRequest<MutableList<ClearanceOverride>> = object : ApiRequest<MutableList<ClearanceOverride>>("/server/${guild.id}/overrides") {
            override fun parse(json: JSONObject): MutableList<ClearanceOverride> {
                val overrides = mutableListOf<ClearanceOverride>()
                json.getJSONArray("overrides").map { it as JSONObject }.forEach { j ->
                    overrides.add(ClearanceOverride(j.getInt("id"), j.getString("command"), j.getString("clearance")))
                }
                return overrides
            }
        }
    }
}

class VoidApiResponse