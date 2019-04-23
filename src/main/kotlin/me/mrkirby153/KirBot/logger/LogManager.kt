package me.mrkirby153.KirBot.logger

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.database.models.guild.LogSettings
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.CustomEmoji
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.convertSnowflake
import me.mrkirby153.KirBot.utils.crypto.AesCrypto
import me.mrkirby153.KirBot.utils.escapeMentions
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.resolveMentions
import me.mrkirby153.KirBot.utils.uploadToArchive
import me.mrkirby153.KirBot.utils.urlEscape
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

class LogManager(private val guild: KirBotGuild) {
    private var logChannels = Model.where(LogSettings::class.java, "server_id", guild.id).get()
    private val channelLoggers = ConcurrentHashMap<String, ChannelLogger>()

    var hushed = false

    fun reloadLogChannels() {
        Bot.LOG.debug("Reloading log channels on $guild")
        this.logChannels = Model.where(LogSettings::class.java, "server_id", guild.id).get()
        updateLoggers()
    }

    private fun updateLoggers() {
        channelLoggers.entries.removeIf { it.key !in this.guild.textChannels.map { it.id } }
        this.guild.textChannels.forEach { chan ->
            val logger = channelLoggers.getOrPut(chan.id) { ChannelLogger(this.guild, chan.id) }
            val settings = this.logChannels.firstOrNull { it.channelId == chan.id }

            if (settings != null) // If we have log events to subscribe to
                logger.setSubscriptions(*LogEvent.values().filter {
                    /*
                        If the user has nothing included, include everything except the excluded
                        If the user has things included, include the things included & nothing else
                     */
                    if (settings.included == 0L) {
                        !LogEvent.has(settings.excluded, it)
                    } else {
                        LogEvent.has(settings.included, it)
                    }
                }.toTypedArray())
        }
    }

    fun logMessageDelete(id: String) {
        if (hushed)
            return
        val msg = Model.where(GuildMessage::class.java, "id", id).first() ?: return

        val author = Bot.shardManager.getUser(msg.author) ?: return
        val chan = guild.getTextChannelById(msg.channel) ?: return

        val ignored = guild.extraData.optJSONArray("log-ignored")?.map { it.toString() }
        if (ignored != null && author.id in ignored)
            return // The user is in the ignored log array

        this.genericLog(LogEvent.MESSAGE_DELETE, ":wastebasket:",
                "${author.logName} message deleted in **#${chan.name}** \n ${msg.message.resolveMentions().escapeMentions().urlEscape()}" + buildString {
                    val attachments = msg.attachments
                    if (attachments != null) {
                        val split = attachments.split(",")
                        append(" (")
                        append(split.joinToString(", ") { "<${it.trim()}>" })
                        append(")")
                    }
                })
    }

    fun logBulkDelete(chan: TextChannel, messages: List<String>) {
        if (hushed)
            return
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
        val query = "SELECT `server_messages`.`id` as `message_id`, `server_messages`.`server_id`, `author` as 'author_id', `channel`, `message`, `username`, `discriminator`, `attachments` FROM `server_messages` LEFT JOIN `seen_users` ON `server_messages`.`author` = `seen_users`.`id` LEFT JOIN `attachments` ON `server_messages`.`id` = `attachments`.`id` WHERE `server_messages`.`id` IN ($realString)"
        val results = DB.getResults(query, *(messages.toTypedArray()))
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
                            authorId, username, msg,
                            LogManager.decrypt(result.getString("attachments") ?: "")))
        }
        val archiveUrl = if (msgs.isNotEmpty()) uploadToArchive(
                LogManager.encrypt(msgs.joinToString("\n"))) else ""
        this.genericLog(LogEvent.MESSAGE_BULKDELETE, ":wastebasket:",
                "${messages.size} messages deleted in **#${chan.name}**" + if (archiveUrl.isNotEmpty()) " ($archiveUrl)" else "")
    }

    fun logEdit(message: Message) {
        val old = Model.where(GuildMessage::class.java, "id", message.id).first() ?: return
        val user = message.author
        val ignored = guild.extraData.optJSONArray("log-ignored")?.map { it.toString() }
        if (ignored != null && user.id in ignored)
            return // The user is in the ignored log array
        val oldMessage = old.message
        if (oldMessage.equals(message.contentRaw, true)) {
            return
        }

        this.genericLog(LogEvent.MESSAGE_EDIT, ":pencil:",
                "${user.logName} message edited in **#${message.textChannel.name}** \n **B:** ${oldMessage.resolveMentions().escapeMentions().urlEscape()} \n **A:** ${message.contentRaw.resolveMentions().escapeMentions().urlEscape()}")
    }

    fun genericLog(logEvent: LogEvent, emoji: CustomEmoji,
                   message: String) {
        genericLog(logEvent, emoji.toString(), message)
    }

    fun genericLog(logEvent: LogEvent, emoji: String, message: String) {
        if (!guild.ready)
            return
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone(SettingsRepository.get(guild, "log_timezone", "UTC"))
        val m = buildString {
            append("`[")
            append(sdf.format(System.currentTimeMillis()))
            append("]` ")
            append(emoji)
            append(" $message")
        }
        if (m.length > 2000)
            return // Drop the message as it's over 2k chars
        submitEvent(LogMessage(logEvent, m))
    }

    fun process() {
        this.channelLoggers.forEach { chan, logger ->
            logger.log()
        }
    }

    private fun submitEvent(event: LogMessage) {
        this.channelLoggers.values.forEach {
            it.submitEvent(event)
        }
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