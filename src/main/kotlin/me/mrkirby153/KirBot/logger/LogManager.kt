package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Database
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.CustomEmoji
import me.mrkirby153.KirBot.utils.convertSnowflake
import me.mrkirby153.KirBot.utils.escapeMentions
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.uploadToArchive
import me.mrkirby153.kcutils.use
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
        val selector = "in (${messages.joinToString(",") { "'$it'" }})"
        val query = "SELECT `server_messages`.`id` as 'message_id', `server_messages`.`server_id`, `author` as 'author_id', `channel`, `message`, `user_name`, `user_discrim` FROM `server_messages` LEFT JOIN `guild_members` ON `server_messages`.`author` = `guild_members`.`user_id` AND `guild_members`.`server_id` = ${guild.id} WHERE `server_messages`.`id` $selector"
        val msgs = mutableListOf<String>()
        ModuleManager[Database::class.java].database.getConnection().use {
            it.createStatement().use { st ->
                st.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        val msgId = rs.getString("message_id")
                        val serverId = rs.getString("server_id")
                        val authorId = rs.getString("author_id")
                        val channel = rs.getString("channel")
                        val msg = rs.getString("message")
                        val username = rs.getString("user_name") + "#" + rs.getString(
                                "user_discrim")

                        val timeFormatted = SimpleDateFormat("YYYY-MM-DD HH:MM:ss").format(
                                convertSnowflake(msgId))
                        msgs.add(String.format("%s (%s / %s / %s) %s: %s", timeFormatted, serverId,
                                channel, authorId, username, msg))
                    }
                }
            }
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