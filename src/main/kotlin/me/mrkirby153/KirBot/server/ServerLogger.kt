package me.mrkirby153.KirBot.server

import me.mrkirby153.KirBot.listener.LogListener
import me.mrkirby153.KirBot.utils.CachedValue
import me.mrkirby153.KirBot.utils.Time
import me.mrkirby153.KirBot.utils.embed.b
import net.dv8tion.jda.core.entities.Guild
import java.awt.Color
import java.text.SimpleDateFormat

class ServerLogger(val server: Guild) {
    val channel: CachedValue<String> = CachedValue(60 * 1000)

    @JvmOverloads
    fun log(subject: String, message: String, color: Color = Color.ORANGE, vararg fields: LogField) {
        val chanId: String = LogListener.logChannelCache[server.id] ?: return
        if (chanId.isNotEmpty())
            server.getTextChannelById(chanId)?.sendMessage(buildString {
                append("\u2500".repeat(45))
                appendln("\nTime: *${SimpleDateFormat(Time.DATE_FORMAT_NOW + " Z").format(System.currentTimeMillis())}*")
                appendln(b(subject))
                appendln(message)
                fields.forEach {
                    append(b(it.name))
                    append(": ")
                    appendln(it.value)
                }
            })?.queue()
    }
}

data class LogField(val name: String, val value: String, val inline: Boolean)