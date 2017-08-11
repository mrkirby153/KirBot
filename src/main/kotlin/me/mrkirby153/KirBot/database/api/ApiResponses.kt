package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.User

data class Realname(val firstName: String, val lastName: String) : ApiResponse

data class Realnames(val map: MutableMap<User, Realname?>) : ApiResponse

data class GuildCommand(val name: String, val data: String, val clearance: Clearance, val respectWhitelist: Boolean) : ApiResponse

class GuildCommands(val cmds: List<GuildCommand>) : ApiResponse {

    fun findCommand(name: String): GuildCommand? {
        cmds.forEach {
            if (it.name.equals(name, true))
                return it
        }
        return null
    }
}

data class GuildSettings(val name: String, val realnameSetting: RealnameSetting, val requireRealname: Boolean, val cmdDiscriminator: String,
                         val logChannel: String?, val whitelistedChannels: List<String>) : ApiResponse

enum class ChannelType {
    VOICE,
    TEXT
}

data class GuildChannel(val id: String, val guild: String, val name: String, val type: ChannelType) : ApiResponse

data class GuildChannels(val text: List<GuildChannel>, val voice: List<GuildChannel>) : ApiResponse

class MusicSettings(val enabled: Boolean, whitelist: String, val channels: Array<String>, val blacklistedSongs: Array<String>,
                    val maxQueueLength: Int, val playlists: Boolean, val maxSongLength: Int, val skipCooldown: Int, val skipTimer: Int) : ApiResponse {

    val whitelistMode = WhitelistMode.valueOf(whitelist)

    enum class WhitelistMode {
        OFF,
        BLACKLIST,
        WHITELIST
    }
}

class VoidApiResponse : ApiResponse