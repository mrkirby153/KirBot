package me.mrkirby153.KirBot.server

import me.mrkirby153.KirBot.utils.CachedValue
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import java.awt.Color

class ServerLogger(val server: Guild) {
    val channel: CachedValue<MessageChannel> = CachedValue(60 * 1000)

    @JvmOverloads
    fun log(subject: String, message: String, color: Color = Color.ORANGE) {
/*        val chan = channel.get()
        if (chan == null) {
            val dataChan = Database.getLoggingChannel(server) ?: return
            channel.set(server.getTextChannelById(dataChan))
            log(subject, message)
            return
        }
        chan.send().embed(subject){
            this.color = color
            description = message
        }.rest().queue()*/
    }
}