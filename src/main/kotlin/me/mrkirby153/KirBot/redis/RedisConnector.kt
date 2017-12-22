package me.mrkirby153.KirBot.redis

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.Time
import org.json.JSONObject

private const val MAX_RETRIES = 4

object RedisConnector {

    var running = true

    var retries = 0

    fun get() = Bot.redisConnection.get()

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
            while(running) {
                try {
                    get().use {
                        retries = 0
                        it.psubscribe(RedisHandler(), "kirbot:*")
                    }
                } catch (e: Exception) {
                    if(retries > MAX_RETRIES){
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