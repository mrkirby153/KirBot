package me.mrkirby153.KirBot.logger

import co.aikar.idb.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.CustomEmoji
import me.mrkirby153.KirBot.utils.convertSnowflake
import me.mrkirby153.KirBot.utils.escapeMentions
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.uploadToArchive
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedList
import java.util.TimeZone

class LogManager(private val guild: KirBotGuild) {

    private val logQueue = LinkedList<String>()

    val logChannel: TextChannel?
        get() {
            val chanId = guild.kirbotGuild.settings.logChannel ?: return null
            if (chanId.isEmpty())
                return null
            return guild.getTextChannelById(chanId)
        }

    fun logMessageDelete(id: String) {
        val msg = Model.first(GuildMessage::class.java, Pair("id", id)) ?: return

        val author = Bot.shardManager.getUser(msg.author) ?: return
        val chan = guild.getTextChannelById(msg.channel) ?: return

        val ignored = guild.extraData.optJSONArray("log-ignored")?.map { it.toString() }
        if (ignored != null && author.id in ignored)
            return // The user is in the ignored log array

        this.genericLog(":wastebasket:",
                "${author.nameAndDiscrim}(`${author.id}`) Message deleted in **#${chan.name}** \n ${msg.message.escapeMentions()}")
    }

    fun logBulkDelete(chan: TextChannel, messages: List<String>) {
        if(logChannel == null)
            return // Don't bother creating an archive if logging is disabled
        val selector = "?, ".repeat(messages.size)
        val realString = selector.substring(
                0, selector.lastIndexOf(","))
        val results = DB.getResults(
                "SELECT `server_messages`.`id` as `message_id`, `server_messages`.`server_id`, `author` as 'author_id', `channel`, `message`, `username`, `discriminator` FROM `server_messages` LEFT JOIN `seen_users` ON `server_messages`.`author` = `seen_users`.`id` WHERE `server_messages`.`id` IN ($realString)", *(messages.toTypedArray()))
        val msgs = mutableListOf<String>()
        results.forEach { result ->
            val msgId = result.getString("message_id")
            val serverId = result.getString("server_id")
            val authorId = result.getString("author_id")
            val channel = result.getString("channel")
            val msg = result.getString("message")
            val username = result.getString("username") + "#" + result.getInt(
                    "discriminator")
            val timeFormatted = SimpleDateFormat("YYYY-MM-DD HH:MM:ss").format(
                    convertSnowflake(msgId))

            msgs.add(String.format("%s (%s / %s / %s) %s: %s", timeFormatted, serverId, channel,
                    authorId, username, msg))
        }
        val archiveUrl = if (msgs.isNotEmpty() && logChannel != null) uploadToArchive(
                msgs.joinToString("\n")) else ""
        this.genericLog(":wastebasket:",
                "${messages.size} messages deleted in **#${chan.name}**" + if (archiveUrl.isNotEmpty()) " ($archiveUrl)" else "")
    }

    fun logEdit(message: Message) {
        val old = Model.first(GuildMessage::class.java, Pair("id", message.id)) ?: return
        val user = message.author
        val ignored = guild.extraData.optJSONArray("log-ignored")?.map { it.toString() }
        if (ignored != null && user.id in ignored)
            return // The user is in the ignored log array
        if (old.message.equals(message.contentDisplay, true)) {
            return
        }

        this.genericLog(":pencil:",
                "${user.nameAndDiscrim} Message edited in **#${message.textChannel.name}** \n **B:** ${old.message} \n **A:** ${message.contentRaw.escapeMentions()}")
    }

    fun genericLog(emoj: CustomEmoji, message: String) {
        genericLog(emoj.toString(), message)
    }

    fun genericLog(emoji: String, message: String) {
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
        logQueue.addLast(m)
    }

    fun processQueue() {
        if (logQueue.isEmpty())
            return
        val string = buildString {
            while (logQueue.isNotEmpty()) {
                if (logQueue.peek().length + length > 2000)
                    return@buildString
                appendln(logQueue.pop())
            }
        }
        if (string.isNotBlank())
            logChannel?.sendMessage(string)?.queue()
    }
}