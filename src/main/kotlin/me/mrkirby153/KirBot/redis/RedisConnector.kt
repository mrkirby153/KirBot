package me.mrkirby153.KirBot.redis

import io.sentry.Sentry
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.redis.RedisConnection
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.sharding.ShardManager

private const val MAX_RETRIES = 4

class RedisConnector(val shardManager: ShardManager, val connector: RedisConnection) {

    var running = true

    var retries = 0

    fun listen() {
        val thread = Thread {
            while (running) {
                try {
                    connector.get().use {
                        retries = 0
                        it.subscribe(RedisHandler(shardManager), "kirbot")
                    }
                } catch (e: Exception) {
                    Sentry.capture(e)
                    if (retries > MAX_RETRIES) {
                        running = false
                        Bot.LOG.error("Reached Max retry count, giving up.")
                    } else {
                        val waitTime = (2000 * (Math.pow(2.0, (retries++).toDouble()))).toLong()
                        Bot.LOG.error(
                                "Redis listener encountered an exception, retrying in ${Time.format(
                                        1, waitTime)}")
                        Thread.sleep(waitTime)
                    }
                }
            }
        }
        thread.name = "Redis PubSub Listener"
        thread.isDaemon = true
        thread.start()
    }
}