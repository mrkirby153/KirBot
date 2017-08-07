package me.mrkirby153.KirBot.server

import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.utils.CachedValue
import me.mrkirby153.KirBot.utils.embed.embed
import net.dv8tion.jda.core.entities.Guild
import java.awt.Color

class ServerLogger(val server: Guild) {
    val channel: CachedValue<String> = CachedValue(60 * 1000)

    @JvmOverloads
    fun log(subject: String, message: String, color: Color = Color.ORANGE, vararg fields: LogField) {
        var chanId = channel.get()
        if (chanId == null) {
            chanId = Database.getLoggingChannel(server) ?: return
        }
        server.getTextChannelById(chanId)?.sendMessage(embed(subject) {
            setDescription(message)
            setColor(color)
            fields.forEach {
                addField(it.name, it.value, it.inline)
            }
        }.build())?.queue()
    }
}

data class LogField(val name: String, val value: String, val inline: Boolean)