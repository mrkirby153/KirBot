package com.mrkirby153.kirbot.services.modlog

import com.mrkirby153.kirbot.entity.repo.LogChannelRepository
import com.mrkirby153.kirbot.events.AllShardsReadyEvent
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import org.springframework.context.event.EventListener
import org.springframework.core.task.TaskExecutor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import javax.annotation.PreDestroy

class ModlogManager(private val taskExecutor: TaskExecutor,
                    private val logChannelRepository: LogChannelRepository,
                    private val shardManager: ShardManager) : ModlogService {

    private val log = LogManager.getLogger()
    private val channelLoggers = ConcurrentHashMap<String, MutableList<ChannelLogger>>()
    private val loggerUpdater = LoggerUpdater(this)

    private val hushed = CopyOnWriteArraySet<String>()


    override fun cache(guild: Guild) {
        log.debug("Caching log channels for {}", guild)
        val loggerEntities = logChannelRepository.getAllByServerId(guild.id)
        val channelLoggers = channelLoggers.computeIfAbsent(guild.id) { mutableListOf() }
        loggerEntities.forEach { entity ->
            val existing = channelLoggers.find { it.channelId == entity.channelId }

            val included = LogEvent.decode(entity.included)
            val excluded = LogEvent.decode(entity.excluded)
            log.debug("Loading channel {} (Include: {}, Exclude: {}}", entity.channelId, included,
                    excluded)

            val events = mutableListOf<LogEvent>()

            // If there are no events included, add all events except ones that are excluded
            if (included.isEmpty()) {
                events.addAll(LogEvent.values().filter { it in excluded })
            } else {
                events.addAll(included)
            }

            if (existing != null) {
                log.debug("Updating subscriptions for {} to {}", existing.channelId, events)
                existing.updateSubscriptions(events)
            } else {
                log.debug("Subscribing {} to {}", entity.channelId, events)
                channelLoggers.add(
                        ChannelLogger(shardManager, entity.serverId, entity.channelId, events))
            }
        }
    }

    override fun hush(guild: Guild, hushed: Boolean) {
        if(hushed) {
            this.hushed.add(guild.id)
        } else {
            this.hushed.remove(guild.id)
        }
    }

    override fun log(event: LogEvent, guild: Guild, message: String) {
        if(this.hushed.contains(guild.id) && event.hushable)
            return
        channelLoggers[guild.id]?.forEach { it.submit(message, event) }
    }

    @EventListener
    fun ready(event: AllShardsReadyEvent) {
        log.info("Starting logger updater")
        taskExecutor.execute(loggerUpdater)
    }

    @PreDestroy
    fun destroy() {
        log.info("Shutting down LoggerUpdater")
        loggerUpdater.running = false
    }

    @EventListener
    fun onGuildLeave(event: GuildLeaveEvent) {
        log.debug("Left guild {}. Unregistering channel loggers", event.guild)
        channelLoggers.remove(event.guild.id)
    }


    private class LoggerUpdater(val manager: ModlogManager) : Runnable {
        private val log = LogManager.getLogger()

        var running = true

        private val syncObject = Object()

        override fun run() {
            while (running) {
                try {
                    val loggers = manager.channelLoggers.flatMap { it.value }
                    if (loggers.any { it.hasPendingMessages() }) {
                        val toExecute = loggers.filter { it.hasPendingMessages() }
                        log.debug("{} loggers to execute", toExecute.size)
                        toExecute.forEach { it.log() }
                    } else {
                        log.debug("No pending log events. Waiting")
                        synchronized(syncObject) {
                            syncObject.wait()
                        }
                        continue // We don't want to wait 500ms
                    }
                } catch (e: Throwable) {
                    log.error("Encountered an exception while running", e)
                }
                Thread.sleep(500)
            }
        }

        fun notifyUpdater() {
            synchronized(syncObject) {
                syncObject.notifyAll()
            }
        }
    }
}