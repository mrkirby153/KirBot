package me.mrkirby153.KirBot.sharding

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.listener.ShardListener
import net.dv8tion.jda.core.JDA

class Shard(val id: Int, private val jda: JDA, val bot: Bot) : JDA by jda {
    init {
        addEventListener(ShardListener(this, bot))
    }

    override fun toString(): String {
        return "Shard(id=$id)"
    }
}