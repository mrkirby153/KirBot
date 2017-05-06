package me.mrkirby153.KirBot

import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.listener.ShardListener
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild

class Shard(val id: Int, private val jda: JDA, val bot: Bot) : JDA by jda {

    init {
        addEventListener(ShardListener(this, bot))
    }

    val serverData = mutableMapOf<Long, ServerData>()

    fun getServerData(id: Long): ServerData {
        return serverData.getOrPut(id) { ServerData(id, this) }
    }

    fun getServerData(guild: Guild) = getServerData(guild.idLong)

    override fun shutdown() {
        jda.shutdown(false)
    }
}