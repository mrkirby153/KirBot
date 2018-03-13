package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import net.dv8tion.jda.core.entities.TextChannel
import java.text.SimpleDateFormat

class AdminControl : Module("admin") {

    val logChannel: TextChannel? by lazy {
        Bot.shardManager.shards.flatMap { it.guilds }.forEach { guild ->
            if (Bot.properties.getProperty("control-channel") == null)
                return@lazy null
            if (guild.getTextChannelById(Bot.properties.getProperty("control-channel")) != null)
                return@lazy guild.getTextChannelById(Bot.properties.getProperty("control-channel"))
        }
        return@lazy null
    }

    override fun onLoad() {

    }

    fun log(message: String) {
        val msg = buildString {
            append("[`")
            append(SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()))
            append("`] $message")
        }
        logChannel?.sendMessage(msg)?.queue()
    }

    companion object {
        fun log(msg: String) {
            ModuleManager[AdminControl::class.java].log(msg)
        }
    }
}