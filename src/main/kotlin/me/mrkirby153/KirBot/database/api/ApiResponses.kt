package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.user.Clearance

data class Realname(val firstName: String, val lastName: String)

data class GuildCommand(val name: String, val data: String, val clearance: Clearance, val respectWhitelist: Boolean)

data class GuildSettings(val name: String, val realnameSetting: RealnameSetting, val requireRealname: Boolean, val cmdDiscriminator: String,
                         val logChannel: String?, val whitelistedChannels: List<String>)

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

class GuildRole(val id: String, val name: String, val serverId: String) {
    val guild = Bot.getGuild(serverId)
    val role = guild?.getRoleById(id)
}

class VoidApiResponse