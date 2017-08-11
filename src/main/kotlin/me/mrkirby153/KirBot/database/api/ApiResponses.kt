package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.User

data class Realname(val firstName: String, val lastName: String) : ApiResponse

data class Realnames(val map: MutableMap<User, Realname>) : ApiResponse

data class GuildCommand(val name: String, val data: String, val clearance: Clearance, val respectWhitelist: Boolean) : ApiResponse

data class GuildCommands(val cmds: List<GuildCommand>) : ApiResponse

data class GuildSettings(val name: String, val realnameSetting: String, val requireRealname: Boolean, val cmdDiscriminator: String,
                         val logChannel: String?, val whitelistedChannels: List<String>): ApiResponse

enum class ChannelType{
    VOICE,
    TEXT
}

data class GuildChannel(val id: String, val guild: String, val name: String, val type: ChannelType): ApiResponse

data class GuildChannels(val text: List<GuildChannel>, val voice: List<GuildChannel>): ApiResponse

class VoidApiResponse: ApiResponse