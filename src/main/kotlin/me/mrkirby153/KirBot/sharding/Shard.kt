package me.mrkirby153.KirBot.sharding

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.api.ClearanceOverride
import me.mrkirby153.KirBot.database.api.GuildCommand
import me.mrkirby153.KirBot.database.api.GuildSettings
import me.mrkirby153.KirBot.listener.LogListener
import me.mrkirby153.KirBot.listener.ShardListener
import me.mrkirby153.KirBot.utils.redis.GenericWrapper
import me.mrkirby153.KirBot.utils.redis.RedisDataStore
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild

class Shard(val id: Int, private val jda: JDA, val bot: Bot) : JDA by jda {

    init {
        addEventListener(ShardListener(this, bot))
        addEventListener(LogListener(this))
    }

    val serverData = mutableMapOf<Long, ServerData>()

    val serverSettings = RedisDataStore(GuildSettings::class.java, "settings")

    val customCommands = RedisDataStore(CommandWrapper::class.java, "commands")

    val clearanceOverrides = RedisDataStore(
            ClearanceWrapper::class.java, "overrides")

    fun getServerData(id: Long): ServerData {
        return serverData.getOrPut(id) { ServerData(id, this) }
    }

    fun getServerData(guild: Guild) = getServerData(guild.idLong)

    fun loadSettings() {
        // Load up settings for all the shards on the guild
        guilds.forEach { guild ->
            loadSettings(guild)
        }
    }

    fun loadSettings(guild: Guild) {
        GuildSettings.get(guild).queue {
            serverSettings[guild.id] = it
        }
        GuildCommand.getCommands(guild).queue {
            customCommands[guild.id] = CommandWrapper(
                    it.toMutableList())
        }
        ClearanceOverride.get(guild).queue {
            clearanceOverrides[guild.id] = ClearanceWrapper(it)
        }
    }

    override fun shutdown() {
        serverData.clear()
        serverSettings.clear()
        customCommands.clear()
        clearanceOverrides.clear()
        jda.shutdown()
    }

    class CommandWrapper(list: MutableList<GuildCommand>) :
            GenericWrapper<MutableList<GuildCommand>>(list)

    class ClearanceWrapper(list: MutableList<ClearanceOverride>) :
            GenericWrapper<MutableList<ClearanceOverride>>(list)
}