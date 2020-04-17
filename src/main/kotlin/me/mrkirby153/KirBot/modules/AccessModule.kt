package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import net.dv8tion.jda.api.entities.Guild
import javax.inject.Inject

class AccessModule @Inject constructor(private val redis: Redis): Module("access") {

    private val REDIS_KEY = "guilds"

    init {
        dependencies.add(Redis::class.java)
    }

    override fun onLoad() {

    }


    fun onList(guild: Guild, list: WhitelistMode): Boolean {
        redis.getConnection().use {
            return WhitelistMode.getMode(it.zscore(REDIS_KEY, guild.id)?.toInt() ?: 0) == list
        }
    }


    fun addToList(guild: String, mode: WhitelistMode) {
        redis.getConnection().use {
            it.zadd(REDIS_KEY, mode.score.toDouble(), guild)
        }
    }

    fun removeFromList(guild: String, mode: WhitelistMode) {
        redis.getConnection().use {
            it.zrem(REDIS_KEY, guild)
        }
    }

    fun getList(mode: WhitelistMode): Set<String> {
        redis.getConnection().use {
            return it.zrangeByScore(REDIS_KEY, mode.score.toDouble(), mode.score.toDouble())
        }
    }


    enum class WhitelistMode(val score: Int) {
        NEUTRAL(0),
        WHITELIST(1),
        BLACKLIST(2);

        companion object {
            fun getMode(score: Int): WhitelistMode {
                return values().firstOrNull { it.score == score } ?: NEUTRAL
            }
        }
    }
}