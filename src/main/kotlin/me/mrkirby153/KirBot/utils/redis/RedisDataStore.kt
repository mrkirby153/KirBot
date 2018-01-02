package me.mrkirby153.KirBot.utils.redis

import com.google.common.cache.CacheBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.utils.DataStore
import java.util.concurrent.TimeUnit

class RedisDataStore<V>(val clazz: Class<V>, val key: String) : DataStore<String, V> {

    private val gson: Gson = GsonBuilder().create()

    val cache = CacheBuilder.newBuilder().expireAfterWrite(250,
            TimeUnit.MILLISECONDS).build<String, V>()

    override fun clear() {
        Bot.redisConnection.get().use {
            val keys = it.keys("$key:*")
            if (keys.isNotEmpty())
                it.del(*keys.toTypedArray())
        }
    }

    override fun containsKey(key: String): Boolean {
        Bot.redisConnection.get().use {
            return get(key) != null
        }
    }

    override fun containsValue(value: V): Boolean {
        Bot.redisConnection.get().use {
            it.keys("$key:*").forEach { key ->
                if (get(key) == value)
                    return true
            }
        }
        return false
    }

    override operator fun get(key: String): V? {
        return cache.getIfPresent(key) ?:
                Bot.redisConnection.get().use {
                    val data = decode(it.get("${this.key}:$key"))
                    if (data != null)
                        cache.put(key, data)
                    return data
                }
    }

    override fun remove(key: String): V? {
        if (!containsKey(key))
            return null
        Bot.redisConnection.get().use {
            val toReturn = get(key)
            it.del("${this.key}:$key")
            return toReturn
        }
    }

    override operator fun set(key: String, value: V): V? {
        val toReturn = if (containsKey(key)) get(key) else null

        Bot.redisConnection.get().use {
            it.set("${this.key}:$key", encode(value))
        }
        return toReturn
    }

    override fun size(): Int {
        Bot.redisConnection.get().use {
            return it.keys("$key:*").size
        }
    }

    private fun encode(data: V) = gson.toJson(data)

    private fun decode(json: String?): V? {
        Bot.LOG.debug("Decoding: $json")
        return gson.fromJson(json, clazz)
    }
}