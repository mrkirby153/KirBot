package me.mrkirby153.KirBot.backfill

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Database
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.toSnowflake
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object RecoveryManager {

    fun globalRecovery(duration: Long, pool: Int = 10): ActiveRecovery {
        val threadPool = Executors.newFixedThreadPool(pool)
        val channels = Bot.shardManager.textChannels.filter {
            it.checkPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)
        }
        Bot.LOG.info("!!! Global Recovery Started (${channels.size} channels) !!!")
        val start = Instant.now().minusMillis(duration)
        val end = Instant.now()
        val ar = ActiveRecovery(channels, threadPool)
        channels.forEach {
            val task = RecoveryTask(ar, it, start, end)
            threadPool.submit(task)
        }
        return ar
    }

    fun guildRecovery(guild: Guild, duration: Long, pool: Int = 10): ActiveRecovery {
        Bot.LOG.info("!!! Guild recovery started for guild $guild !!!")
        val threadPool = Executors.newFixedThreadPool(pool)
        val channels = guild.textChannels.filter {
            it.checkPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)
        }
        val start = Instant.now().minusMillis(duration)
        val end = Instant.now()
        val ar = ActiveRecovery(channels, threadPool)
        channels.forEach {
            val task = RecoveryTask(ar, it, start, end)
            threadPool.submit(task)
        }
        return ar
    }
}

data class ActiveRecovery(val channels: List<TextChannel>, val pool: ExecutorService,
                          val completed: CopyOnWriteArrayList<TextChannel> = CopyOnWriteArrayList(),
                          var recoveredMessages: Long = 0L)

class RecoveryTask(private val recovery: ActiveRecovery, val channel: TextChannel,
                   val start: Instant,
                   val end: Instant) : Runnable {

    override fun run() {
        Bot.LOG.info("Recovering messages in $channel")
        try {
            val action = channel.getHistoryAfter(toSnowflake(Date(start.toEpochMilli())),
                    100).complete()
            if (action.isEmpty) {
                Bot.LOG.debug("Skipping recovery on $channel - No messages exist")
                recovery.completed.add(channel)
                return
            }

            while (action.retrieveFuture(100).complete().size != 0) {
                // Retrieve all the messages up to the end
                Bot.LOG.debug("\t - Currently at ${action.retrievedHistory.size}")
            }
            Bot.LOG.debug("Retrieved ${action.retrievedHistory.size} messages")

            val connection = ModuleManager[Database::class.java].database.getConnection()
            connection.use {
                val ps = connection.prepareStatement(
                        "INSERT IGNORE INTO `server_messages` (id, server_id, author, channel, message, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)")
                ps.use {
                    try {
                        action.retrievedHistory.forEach { msg ->
                            Bot.LOG.debug("Queueing $msg")
                            if (msg.timeCreated.toInstant().isAfter(end))
                                return@forEach
                            ps.setString(1, msg.id)
                            ps.setString(2, msg.guild.id)
                            ps.setString(3, msg.author.id)
                            ps.setString(4, msg.channel.id)
                            ps.setString(5, LogManager.encrypt(msg.contentRaw))
                            ps.setTimestamp(6, Timestamp(msg.timeCreated.toEpochSecond() * 1000))

                            val edited = msg.timeEdited ?: msg.timeCreated
                            ps.setTimestamp(7, Timestamp(edited.toEpochSecond() * 1000))
                            ps.addBatch()
                            if (msg.attachments.isNotEmpty()) {
                                val attachmentPs = connection.prepareStatement(
                                        "INSERT IGNORE INTO attachments (id, attachments) VALUE (?, ?)")
                                attachmentPs.use {
                                    attachmentPs.setString(1, msg.id)
                                    attachmentPs.setString(2, LogManager.encrypt(
                                            msg.attachments.joinToString(", ") { it.url }))
                                    attachmentPs.executeUpdate()
                                }
                            }
                        }
                    } finally {
                        synchronized(recovery) {
                            recovery.recoveredMessages += ps.executeBatch().sum()
                        }
                        Bot.LOG.debug("Recovery completed on $channel")
                        recovery.completed.add(channel)
                    }
                }
            }
        } catch (e: Exception) {
            Bot.LOG.error("An error occurred when recovering $channel")
            e.printStackTrace()
            recovery.completed.add(channel)
        }
    }

}