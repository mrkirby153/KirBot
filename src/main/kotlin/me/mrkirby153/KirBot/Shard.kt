package me.mrkirby153.KirBot

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.api.GuildSettings
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.listener.AntiSpamListener
import me.mrkirby153.KirBot.listener.LogListener
import me.mrkirby153.KirBot.listener.ShardListener
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import java.util.concurrent.TimeUnit

class Shard(val id: Int, private val jda: JDA, val bot: Bot) : JDA by jda {

    init {
        addEventListener(ShardListener(this, bot))
        addEventListener(AntiSpamListener(this))
        addEventListener(LogListener(this))
    }

    val serverData = mutableMapOf<Long, ServerData>()

    val serverSettings = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build(
            object : CacheLoader<String, GuildSettings>() {
                override fun load(key: String): GuildSettings {
                    return PanelAPI.guildSettings(jda.getGuildById(key)).execute()
                }
            }
    )

    fun getServerData(id: Long): ServerData {
        return serverData.getOrPut(id) { ServerData(id, this) }
    }

    fun getServerData(guild: Guild) = getServerData(guild.idLong)

    override fun shutdown() {
        jda.shutdown(false)
    }
}