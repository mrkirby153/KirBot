package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.KirBotGuild
import net.dv8tion.jda.api.sharding.ShardManager

class LogPump(private val shardManager: ShardManager, private val delay: Long) : Thread() {

    init {
        isDaemon = true
        name = "LogPump"
    }

    private var running = true

    private var shuttingDown = false


    override fun run() {
        Bot.LOG.debug("Log Pump starting up with ($delay ms delay)")
        while (running || shuttingDown) {
            try {
                shardManager.shards.forEach { shard ->
                    shard.guilds.forEach { guild ->
                        KirBotGuild[guild].logManager.process()
                    }
                }
                if (running)
                    sleep(delay)
            } catch (e: Exception) {
                if(e !is InterruptedException)
                    e.printStackTrace()
                // Ignore all exceptions and continue running
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