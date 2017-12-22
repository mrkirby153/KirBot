package me.mrkirby153.KirBot.utils.redis

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisConnection(host: String, port: Int, password: String?,
                      private val dbNumber: Int = 0) {

    private val jedisPool: JedisPool

    init {
        val prevLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = RedisDataStore::class.java.classLoader

        val config = JedisPoolConfig().apply {
            maxWaitMillis = 1000
            minIdle = 1
            testOnBorrow = true
            maxTotal = 20
            blockWhenExhausted = true
        }
        jedisPool = JedisPool(config, host, port, 1000,
                if (password?.isEmpty() == true) null else password)

        Thread.currentThread().contextClassLoader = prevLoader
    }

    fun get(): Jedis = this.jedisPool.resource.apply {
        if(this.db.toInt() != dbNumber)
            this.select(dbNumber)
    }
}