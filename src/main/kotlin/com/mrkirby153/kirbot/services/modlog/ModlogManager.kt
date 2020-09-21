package com.mrkirby153.kirbot.services.modlog

import com.mrkirby153.kirbot.entity.guild.LoggedMessage
import com.mrkirby153.kirbot.entity.guild.repo.LoggedMessageRepository
import com.mrkirby153.kirbot.entity.repo.LogChannelRepository
import com.mrkirby153.kirbot.events.AllShardsReadyEvent
import com.mrkirby153.kirbot.services.ArchiveService
import com.mrkirby153.kirbot.services.UserService
import com.mrkirby153.kirbot.utils.convertSnowflake
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import org.springframework.context.event.EventListener
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import javax.annotation.PreDestroy

@Service
class ModlogManager(private val taskExecutor: TaskExecutor,
                         private val logChannelRepository: LogChannelRepository,
                         private val shardManager: ShardManager,
                         private val loggedMessageRepository: LoggedMessageRepository,
                         private val userService: UserService,
                         private val archiveService: ArchiveService) : ModlogService {

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
                events.addAll(LogEvent.values().filter { it !in excluded })
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
        if (hushed) {
            this.hushed.add(guild.id)
        } else {
            this.hushed.remove(guild.id)
        }
    }

    override fun log(event: LogEvent, guild: Guild, message: String) {
        if (this.hushed.contains(guild.id) && event.hushable)
            return
        channelLoggers[guild.id]?.forEach { it.submit(message, event) }
        if (channelLoggers[guild.id]?.any { it.hasPendingMessages() } == true && loggerUpdater.waiting) {
            log.debug("Notifying waiting logger updater of new messages")
            loggerUpdater.notifyUpdater()
        }
    }


    @EventListener
    fun ready(event: AllShardsReadyEvent) {
        log.info("Starting logger updater")
        taskExecutor.execute(loggerUpdater)
        shardManager.guilds.forEach { cache(it) }
    }

    @PreDestroy
    fun destroy() {
        log.info("Shutting down LoggerUpdater")
        loggerUpdater.running = false
        loggerUpdater.notifyUpdater()
    }

    @EventListener
    fun onGuildLeave(event: GuildLeaveEvent) {
        log.debug("Left guild {}. Unregistering channel loggers", event.guild)
        channelLoggers.remove(event.guild.id)
    }

    @EventListener
    fun onMessageSend(event: GuildMessageReceivedEvent) {
        val msg = loggedMessageRepository.save(LoggedMessage(event.message))
        if(event.message.attachments.isNotEmpty()) {
            msg.attachments = LoggedMessage.MessageAttachments(event.message)
            loggedMessageRepository.save(msg)
        }
    }

    @EventListener
    fun onMessageEdit(event: GuildMessageUpdateEvent) {
        val existing = loggedMessageRepository.findById(event.messageId)
        existing.ifPresent { msg ->
            // Send a log event
            val old = msg.message
            val new = event.message.contentRaw
            val chanName = shardManager.getGuildChannelById(msg.channel)?.name ?: msg.channel
            userService.findUser(msg.author).thenAccept {
                log(LogEvent.MESSAGE_EDIT, event.guild, buildString {
                    append(it.logName)
                    appendln(" message edited in {{**}}$chanName{{**}}")
                    appendln("{{**B:**}} $old\n{{**A:**}} $new")
                })
            }
            msg.update(event.message)
            loggedMessageRepository.save(msg)
        }
    }

    @EventListener
    @Transactional
    fun onMessageDelete(event: GuildMessageDeleteEvent) {
        val existing = loggedMessageRepository.findById(event.messageId)
        existing.ifPresent { msg ->
            val content = msg.message
            val chanName = shardManager.getGuildChannelById(msg.channel)?.name ?: msg.channel
            // TODO: 9/7/20 Filter log ignored users
            userService.findUser(msg.author).thenAccept {
                log(LogEvent.MESSAGE_DELETE, event.guild, buildString {
                    append(it.logName)
                    appendln(" message deleted in {{**}}#$chanName{{**}}")
                    append(content)
                    if (msg.attachments != null && msg.attachments!!.attachments.isNotEmpty()) {
                        append(" (")
                        append(msg.attachments!!.attachments.joinToString(", ") { "<$it>" })
                        append(")")
                    }
                })
            }
            msg.deleted = true
            loggedMessageRepository.save(msg)
        }
    }

    @EventListener
    fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        val existing = loggedMessageRepository.findAllById(event.messageIds)
        val userIds = existing.map { it.author }.toSet()
        userService.findUsers(userIds).thenAccept { users ->
            val userMap = users.map { it.id to it.nameAndDiscriminator }.toMap()
            val timeFormat = SimpleDateFormat("YYYY-MM-dd HH:MM:ss")
            val rawMsg = buildString {
                existing.forEach {
                    val username = userMap.getOrDefault(it.author, it.id)
                    val attachments = it.attachments?.attachments?.joinToString(", ") ?: ""
                    appendln("${
                        timeFormat.format(convertSnowflake(it.id!!))
                    } (${it.serverId} / ${it.channel} / ${it.author}) $username: ${it.message} ($attachments)")
                }
            }
            archiveService.uploadToArchive(rawMsg).thenAccept { url ->
                log(LogEvent.MESSAGE_BULKDELETE, event.guild, "${existing.size} messages deleted in {{**}}#${event.channel.name}{{**}} $url")
            }
        }
        val messageId = existing.mapNotNull { it.id }.toList()
        loggedMessageRepository.setDeleted(messageId)
    }


    private class LoggerUpdater(val manager: ModlogManager) : Runnable {
        private val log = LogManager.getLogger()

        var running = true
        var waiting = false

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
                        waiting = true
                        synchronized(syncObject) {
                            syncObject.wait()
                        }
                        log.debug("Notified")
                        waiting = false
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