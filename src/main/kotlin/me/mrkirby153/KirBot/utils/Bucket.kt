package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis

class Bucket(val keyFormat: String, val maxActions: Int, val timePeriod: Int) {


    fun incr(key: String, amount: Int = 1): Int {
        val k = keyFormat.format(key)
        ModuleManager[Redis::class.java].getConnection().use { con ->
            // Delete old keys
            con.zremrangeByScore(k, "-inf", (System.currentTimeMillis() - timePeriod).toString())
            for (i in 1..amount) {
                con.zadd(k, System.currentTimeMillis().toDouble(), "${System.currentTimeMillis()}-$i")
            }
            con.expire(k, timePeriod / 1000)
            return con.zcount(k, "-inf", "inf").toInt()
        }
    }

    fun check(key: String, amount: Int = 1): Boolean {
        val count = incr(key, amount)
        return count >= maxActions
    }

    fun count(key: String): Int {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            return con.zcount(keyFormat.format(key), "-inf", "inf").toInt()
        }
    }

    fun size(key: String): Double {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            val d = con.zrangeByScore(keyFormat.format(key), "-inf", "inf")
            if (d.size <= 1)
                return 0.0
            return (con.zscore(keyFormat.format(key), d.last()) - con.zscore(keyFormat.format(key),
                    d.first())) / 1000.0
        }
    }

    fun clear(key: String) {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            con.zremrangeByScore(keyFormat.format(key), "-inf", "inf")
        }
    }
}