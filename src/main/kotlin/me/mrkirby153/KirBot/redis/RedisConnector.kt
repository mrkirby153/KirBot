package me.mrkirby153.KirBot.redis

import me.mrkirby153.KirBot.Bot
import org.json.JSONObject
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisConnector {


    val jedisPool: JedisPool

    init {
        val prevClassloader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = RedisConnector::class.java.classLoader
        val config = JedisPoolConfig().apply {
            maxWaitMillis = 1000
            minIdle = 1
            testOnBorrow = true
            maxTotal = 20
            blockWhenExhausted = true
        }

        val password = Bot.properties.getProperty("redis-password", "")
        val host = Bot.properties.getProperty("redis-host", "localhost")
        val port = Bot.properties.getProperty("redis-port", "6379")
        jedisPool = JedisPool(config, host, port.toInt(),
                1000, if (password.isEmpty()) null else password)
        Thread.currentThread().contextClassLoader = prevClassloader
    }

    fun get(): Jedis = jedisPool.resource.apply {
        val dbNumber = Bot.properties.getProperty("redis-db", "0")
        if (this.db.toInt() != dbNumber.toInt())
            select(dbNumber.toInt())
    }

    fun publish(channel: String, message: String) {
        get().use {
            it.publish(channel, message)
        }
    }

    fun publish(channel: String, json: JSONObject) {
        this.publish(channel, json.toString())
    }

    fun listen() {
        val thread = Thread {
            get().use {
                it.psubscribe(RedisHandler(), "kirbot:*")
            }
        }
        thread.name = "Redis PubSub Listener"
        thread.isDaemon = true
        thread.start()
    }
}