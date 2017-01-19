package me.mrkirby153.KirBot

import me.mrkirby153.KirBot.utils.readProperties
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.utils.SimpleLog
import java.util.concurrent.Executors

object Bot {

    @JvmStatic val LOG = SimpleLog.getLog("KirBot")

    lateinit var jda: JDA

    var initialized = false

    val startTime = System.currentTimeMillis()
    val scheduler = Executors.newSingleThreadExecutor()

    val files = BotFiles()

    val properties = files.properties.readProperties()

    val token = "!"


    fun start(token: String) {
        if (initialized)
            throw IllegalStateException("Bot has already been initialized!")
        initialized = true
        LOG.info("Initializing Bot")

        jda = JDABuilder(AccountType.BOT).run {
            setToken(token)
            setAutoReconnect(true)
            buildBlocking()
        }
        jda.addEventListener(EventListener())
        jda.selfUser.manager.setName("KirBot").queue()

        LOG.info("Bot is connecting to discord")
    }

    fun stop() {
        jda.shutdown()
        LOG.info("Bot is disconnecting from Discord")
    }
}