package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.KirBotGuild

class LogPump(private val delay: Long) : Thread() {

    init {
        isDaemon = true
        name = "LogPump"
    }

    private var running = true

    private var shuttingDown = false


    override fun run() {
        Bot.LOG.debug("Log Pump starting up with ($delay ms delay)")
        while (running || shuttingDown) {
            Bot.shardManager.shards.forEach { shard ->
                shard.guilds.forEach { guild ->
                    KirBotGuild[guild].logManager.processQueue()
                }
            }
            try {
                if (running)
                    sleep(delay)
            } catch (e: InterruptedException) {
                // Ignore
            }
            shuttingDown = false
        }
    }

    fun shutdown() {
        running = false
        shuttingDown = true
        interrupt()
    }
}