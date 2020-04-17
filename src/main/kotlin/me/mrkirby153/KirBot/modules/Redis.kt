package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.redis.RedisConnector
import me.mrkirby153.KirBot.utils.redis.RedisConnection
import net.dv8tion.jda.api.sharding.ShardManager
import javax.inject.Inject

class Redis @Inject constructor(private val shardManager: ShardManager): Module("redis") {

    lateinit var redisConnection: RedisConnection

    lateinit var connector: RedisConnector

    override fun onLoad() {
        val password = getProp("redis-password") ?: ""
        val host = getProp("redis-host") ?: "localhost"
        val port = getProp("redis-port")?.toInt() ?: 6379
        val dbNum = getProp("redis-db")?.toInt() ?: 0

        debug("Connecting to redis database $host:$port ($dbNum)")

        this.redisConnection = RedisConnection(host, port,
                if (password.isEmpty()) null else password, dbNum)

        connector = RedisConnector(shardManager, redisConnection)

        connector.listen()
    }

    override fun onUnload() {
        connector.running = false
    }

    fun getConnection() = redisConnection.get()
}