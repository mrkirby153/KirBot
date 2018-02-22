package me.mrkirby153.KirBot.sharding

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.listener.ShardListener
import me.mrkirby153.KirBot.listener.WaitUtilsListener
import net.dv8tion.jda.core.JDA

class Shard(val id: Int, private val jda: JDA, val bot: Bot) : JDA by jda {
    init {
        addEventListener(ShardListener(this, bot))
        addEventListener(WaitUtilsListener())
    }

    override fun toString(): String {
        return "Shard(id=$id)"
    }
}