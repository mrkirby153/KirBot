package me.mrkirby153.KirBot.logger

import co.aikar.idb.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.database.models.guild.LogSettings
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.CustomEmoji
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.convertSnowflake
import me.mrkirby153.KirBot.utils.crypto.AesCrypto
import me.mrkirby153.KirBot.utils.escapeMentions
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.uploadToArchive
import me.mrkirby153.KirBot.utils.urlEscape
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedList
import java.util.TimeZone

class LogManager(private val guild: KirBotGuild) {

    private val logQueue = LinkedList<LogMessage>()

    private var logChannels = Model.get(LogSettings::class.java, Pair("server_id", guild.id))

    fun reloadLogChannels() {
        this.logChannels = Model.get(LogSettings::class.java, Pair("server_id", guild.id))
    }

    fun logMessageDelete(id: String) {
        val msg = Model.first(GuildMessage::class.java, Pair("id", id)) ?: return

        val author = Bot.shardManager.getUser(msg.author) ?: return
        val chan = guild.getTextChannelById(msg.channel) ?: return

        val ignored = guild.extraData.optJSONArray("log-ignored")?.map { it.toString() }
        if (ignored != null && author.id in ignored)
            return // The user is in the ignored log array

        this.genericLog(LogEvent.MESSAGE_DELETE, ":wastebasket:",
                "${author.nameAndDiscrim}(`${author.id}`) Message deleted in **#${chan.name}** \n ${LogManager.decrypt(
                        msg.message).escapeMentions().urlEscape()}" + buildString {
                    if (msg.attachments != null) {
                        append(" (")
                        msg.attachments!!.split(",").joinToString(", ") { "<${it.trim()}>" }
                        append(")")
                    }
                })
    }

    fun logBulkDelete(chan: TextChannel, messages: List<String>) {
        var shouldLog = false
        logChannels.forEach { c ->
            if (shouldLog(LogEvent.MESSAGE_BULKDELETE, c.included, c.excluded))
                shouldLog = true
        }
        if (!shouldLog)
            return
        val selector = "?, ".repeat(messages.size)
        val realString = selector.substring(
                0, selector.lastIndexOf(","))
        val results = DB.getResults(
                "SELECT `server_messages`.`id` as `message_id`, `server_messages`.`server_id`, `author` as 'author_id', `channel`, `message`, `username`, `discriminator`, `attachments` FROM `server_messages` LEFT JOIN `seen_users` ON `server_messages`.`author` = `seen_users`.`id` WHERE `server_messages`.`id` IN ($realString)",
                *(messages.toTypedArray()))
        val msgs = mutableListOf<String>()
        results.forEach { result ->
            val msgId = result.getString("message_id")
            val serverId = result.getString("server_id")
            val authorId = result.getString("author_id")
            val channel = result.getString("channel")
            val msg = LogManager.decrypt(result.getString("message"))
            val username = result.getString("username") + "#" + result.getInt(
                    "discriminator")
            val timeFormatted = SimpleDateFormat("YYYY-MM-dd HH:MM:ss").format(
                    convertSnowflake(msgId))

            msgs.add(
                    String.format("%s (%s / %s / %s) %s: %s (%s)", timeFormatted, serverId, channel,
                            authorId, username, msg, result.getString("attachments") ?: ""))
        }
        val archiveUrl = if (msgs.isNotEmpty()) uploadToArchive(
                LogManager.encrypt(msgs.joinToString("\n"))) else ""
        this.genericLog(LogEvent.MESSAGE_BULKDELETE, ":wastebasket:",
                "${messages.size} messages deleted in **#${chan.name}**" + if (archiveUrl.isNotEmpty()) " ($archiveUrl)" else "")
    }

    fun logEdit(message: Message) {
        val old = Model.first(GuildMessage::class.java, Pair("id", message.id)) ?: return
        val user = message.author
        val ignored = guild.extraData.optJSONArray("log-ignored")?.map { it.toString() }
        if (ignored != null && user.id in ignored)
            return // The user is in the ignored log array
        val oldMessage = LogManager.decrypt(old.message)
        if (oldMessage.equals(message.contentRaw, true)) {
            return
        }

        this.genericLog(LogEvent.MESSAGE_EDIT, ":pencil:",
                "${user.nameAndDiscrim} Message edited in **#${message.textChannel.name}** \n **B:** ${oldMessage.escapeMentions().urlEscape()} \n **A:** ${message.contentRaw.escapeMentions().urlEscape()}")
    }

    fun genericLog(logEvent: LogEvent, emoji: CustomEmoji,
                   message: String) {
        genericLog(logEvent, emoji.toString(), message)
    }

    fun genericLog(logEvent: LogEvent, emoji: String, message: String) {
        if (!guild.ready)
            return
        val timezone = TimeZone.getTimeZone(this.guild.settings.logTimezone)
        val calendar = Calendar.getInstance(timezone)
        val m = buildString {
            append("`[")
            append(SimpleDateFormat("HH:mm:ss").format(calendar.timeInMillis))
            append("]` ")
            append(emoji)
            append(" $message")
        }
        if (m.length > 2000)
            return
        logQueue.addLast(LogMessage(logEvent, m))
    }

    fun processQueue() {
        if (logQueue.isEmpty())
            return
        val toRemove = mutableSetOf<LogMessage>()
        logChannels.forEach { s ->
            // Find events

            val events = LinkedList<LogMessage>()

            logQueue.filter { m ->
                if (s.included == 0L) {
                    // All events are included, check exclusions
                    !LogEvent.has(s.excluded, m.event)
                } else {
                    // We have whitelisted events, use those instead
                    LogEvent.has(s.included, m.event)
                }
            }.toCollection(events)

            val string = buildString {
                while (events.isNotEmpty()) {
                    if (events.peek().message.length + length > 2000)
                        return@buildString
                    val evt = events.pop()
                    appendln(evt.message)

                    toRemove.add(evt)
                }
            }
            if (string.isNotBlank()) {
                val channel = guild.getTextChannelById(s.channelId) ?: return@forEach
                if (channel.checkPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
                    channel.sendMessage(string).queue()
            }
        }
        logQueue.removeIf {
            if (it in toRemove)
                return@removeIf true
            // Remove all events that don't have loggings
            var hasLogChannel = false
            logChannels.forEach { chan ->
                if (shouldLog(it.event, chan.included, chan.excluded)) {
                    hasLogChannel = true
                }
            }
            if (!hasLogChannel)
                Bot.LOG.debug(
                        "[${this.guild.id}] Event ${it.event} has no logging channel, removing")
            !hasLogChannel
        }
        logQueue.removeAll(toRemove)
    }

    private fun shouldLog(event: LogEvent, include: Long, exclude: Long): Boolean {
        return if (include == 0L)
            !LogEvent.has(exclude, event)
        else
            LogEvent.has(include, event)
    }

    data class LogMessage(val event: LogEvent, val message: String)

    companion object {
        fun encrypt(message: String): String {
            if (message.startsWith("e:"))
                return message // Pass through
            return "e:${AesCrypto.encrypt(message)}"
        }

        fun decrypt(message: String): String {
            if (message.startsWith("e:"))
                return AesCrypto.decrypt(message.substring(2))
            return message
        }
    }
}