package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.CustomEmoji
import me.mrkirby153.KirBot.utils.escapeMentions
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.mdEscape
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class LogManager(private val guild: KirBotGuild) {

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
                "${author.nameAndDiscrim}(`${author.id}`) Message deleted in **#${chan.name}** \n ${msg.message.escapeMentions().mdEscape()}")
    }

    fun logBulkDelete(chan: TextChannel, messages: List<String>) {
        this.genericLog(":wastebasket:",
                "${messages.size} messages deleted in **#${chan.name}**")
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
        logChannel?.sendMessage(buildString {
            append("`[")
            append(SimpleDateFormat("HH:mm:ss").format(calendar.timeInMillis))
            append("]` ")
            append(emoji)
            append(" $message")
        })?.queue()
    }
}