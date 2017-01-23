package me.mrkirby153.KirBot.net.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.net.NetworkMessage
import me.mrkirby153.KirBot.net.NetworkMessageHandler
import me.mrkirby153.KirBot.server.ServerRepository
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color

class MinecraftBridge : NetworkMessageHandler {

    override val requireAuth = true

    override fun handle(message: NetworkMessage) {
        val bridgeMessage = ServerRepository.gson.fromJson(message.data, BridgeMessage::class.java)

        val guild = Bot.jda.getGuildById(message.guild) ?: return

       if(guild.textChannels.filter { it.id == bridgeMessage.channel }.count() < 1)
           return
        val channel = guild.getTextChannelById(bridgeMessage.channel)

        val embed = EmbedBuilder().run {
            setDescription(bridgeMessage.message)
            setColor(Color.WHITE)
            setAuthor(bridgeMessage.user, null, "https://minotar.net/helm/${bridgeMessage.user}")
            build()
        }
        channel.sendMessage(embed).queue()
    }
}

data class BridgeMessage(val channel: String, val user: String, val message: String)