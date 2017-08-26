package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject

data class Realname(val firstName: String, val lastName: String)

data class GuildCommand(val name: String, val data: String, val clearance: Clearance, val respectWhitelist: Boolean)

data class GuildSettings(val name: String, val realnameSetting: RealnameSetting, val requireRealname: Boolean, val cmdDiscriminator: String,
                         val logChannel: String?, val whitelistedChannels: List<String>, val managerRoles: List<String>)

enum class ChannelType {
    VOICE,
    TEXT
}

data class GuildChannel(val id: String, val guild: String, val name: String, val type: ChannelType)

data class GuildChannels(val text: List<GuildChannel>, val voice: List<GuildChannel>)

class MusicSettings(val enabled: Boolean, whitelist: String, val channels: Array<String>, val blacklistedSongs: Array<String>,
                    val maxQueueLength: Int, val playlists: Boolean, val maxSongLength: Int, val skipCooldown: Int, val skipTimer: Int) {

    val whitelistMode = WhitelistMode.valueOf(whitelist)

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
}

class GuildRole(val id: String, val name: String, val serverId: String, val permissions: Long) {
    val guild = Bot.getGuild(serverId)
    val role = guild?.getRoleById(id)
}

class Quote(val id: Int, val messageId: String, val user: String, val server: String, val content: String) {
    val guild = Bot.getGuild(server)
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
}

class VoidApiResponse