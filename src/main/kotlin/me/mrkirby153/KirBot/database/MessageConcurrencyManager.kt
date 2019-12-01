package me.mrkirby153.KirBot.database

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Database
import net.dv8tion.jda.api.entities.Message
import java.sql.Timestamp
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor

object MessageConcurrencyManager {

    private val workerPool = Executors.newScheduledThreadPool(5) as ScheduledThreadPoolExecutor

    private val running = CopyOnWriteArrayList<MessageTask>()

    fun queueSize(): Int {
        return workerPool.queue.size
    }

    fun runningJobs(): Int {
        return workerPool.activeCount
    }

    fun messageCount(): Int {
        return running.sumBy { it.message.size }
    }

    fun delete(vararg messages: Message) {
        workerPool.submit(MessageTask(messages, MessageConcurrencyManager.TaskType.DELETE))
    }

    fun delete(vararg messages: String) {
        workerPool.submit(DeleteTask(messages))
    }

    fun insert(vararg messages: Message) {
        val messageTask = MessageTask(messages, MessageConcurrencyManager.TaskType.CREATE)
        this.running.add(messageTask)
        workerPool.submit(messageTask)
    }

    fun update(vararg messages: Message) {
        val messageTask = MessageTask(messages, MessageConcurrencyManager.TaskType.EDIT)
        this.running.add(messageTask)
        workerPool.submit(messageTask)
    }

    class MessageTask(val message: Array<out Message>, val action: TaskType) : Runnable {
        override fun run() {
            try {
                val connection = ModuleManager[Database::class].database.getConnection()
                connection.use {
                    when (action) {
                        MessageConcurrencyManager.TaskType.CREATE -> {
                            val messagePs = connection.prepareStatement(
                                    "INSERT INTO server_messages (id, server_id, author, channel, message, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE message = message, edit_count = edit_count + 1")
                            val attachmentPs = connection.prepareStatement(
                                    "INSERT INTO attachments (id, attachments, created_at, updated_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE attachments = attachments")
                            this.message.forEach { msg ->
                                messagePs.setString(1, msg.id)
                                messagePs.setString(2, msg.guild.id)
                                messagePs.setString(3, msg.author.id)
                                messagePs.setString(4, msg.channel.id)
                                messagePs.setString(5, LogManager.encrypt(msg.contentRaw))
                                messagePs.setTimestamp(6,
                                        Timestamp.from(msg.timeCreated.toInstant()))
                                messagePs.setTimestamp(7,
                                        Timestamp.from(msg.timeCreated.toInstant()))
                                messagePs.addBatch()
                                if (msg.attachments.size > 0) {
                                    attachmentPs.setString(1, msg.id)
                                    attachmentPs.setString(2, LogManager.encrypt(
                                            msg.attachments.joinToString(",") { it.url }))
                                    attachmentPs.setTimestamp(3,
                                            Timestamp.from(msg.timeCreated.toInstant()))
                                    attachmentPs.setTimestamp(4,
                                            Timestamp.from(msg.timeCreated.toInstant()))
                                    attachmentPs.addBatch()
                                }
                            }
                            Bot.LOG.debug(
                                    "Inserting ${message.size} messages: (${message.joinToString(
                                            ",") { it.id }})")
                            messagePs.executeBatch()
                            attachmentPs.executeBatch()
                            messagePs.close()
                            attachmentPs.close()
                        }
                        MessageConcurrencyManager.TaskType.EDIT -> {
                            val updatePs = connection.prepareStatement(
                                    "UPDATE server_messages SET message = ?, edit_count = edit_count + 1, updated_at = CURRENT_TIMESTAMP() WHERE id = ?")
                            this.message.forEach { msg ->
                                updatePs.setString(1, LogManager.encrypt(msg.contentRaw))
                                updatePs.setString(2, msg.id)
                                updatePs.addBatch()
                            }
                            Bot.LOG.debug(
                                    "Editing ${message.size} messages: (${message.joinToString(
                                            ",") { it.id }})")
                            updatePs.executeBatch()
                            updatePs.close()
                        }
                        MessageConcurrencyManager.TaskType.DELETE -> {
                            DeleteTask(message.map { it.id }.toTypedArray()).run()
                        }
                    }
                }
            } finally {
                running.remove(this)
            }
        }
    }

    class DeleteTask(val ids: Array<out String>) : Runnable {
        override fun run() {
            val connection = ModuleManager[Database::class].database.getConnection()
            connection.use {
                val placeholders = ids.joinToString(",") { "?" }
                // If the message already is deleted, don't delete it
                val sql = "UPDATE server_messages SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id IN ($placeholders) AND deleted = 0"
                val updatePs = connection.prepareStatement(sql)
                var i = 1
                ids.forEach { msg ->
                    updatePs.setString(i++, msg)
                }
                try {
                    updatePs.executeUpdate()
                    updatePs.close()
                    Bot.LOG.debug("Deleting ${ids.size} messages: (${ids.contentToString()})")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    enum class TaskType {
        CREATE,
        EDIT,
        DELETE
    }
}