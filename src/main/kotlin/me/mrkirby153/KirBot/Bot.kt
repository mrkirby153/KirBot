package me.mrkirby153.KirBot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.realname.RealnameUpdater
import me.mrkirby153.KirBot.server.ServerRepository
import me.mrkirby153.KirBot.utils.readProperties
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.utils.SimpleLog
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Bot {

    @JvmStatic val LOG = SimpleLog.getLog("KirBot")

    lateinit var jda: JDA

    var initialized = false

    val startTime = System.currentTimeMillis()
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val files = BotFiles()

    val properties = files.properties.readProperties()

    val token = "!"

    val admins: List<String> = files.admins.run { this.readLines() }

    val playerManager: AudioPlayerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())
        registerSourceManager(SoundCloudAudioSourceManager())
        registerSourceManager(VimeoAudioSourceManager())
        registerSourceManager(TwitchStreamAudioSourceManager())
        registerSourceManager(BeamAudioSourceManager())
    }


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

        LOG.info("Starting real name updater thread")
        scheduler.scheduleAtFixedRate(RealnameUpdater(), 60, 60, TimeUnit.SECONDS)

        LOG.info("Bot is connecting to discord")

        LOG.info("Updating names")
        jda.guilds
                .mapNotNull { ServerRepository.getServer(it) }
                .forEach { Database.onJoin(it) }

    }

    fun stop() {
        jda.shutdown()
        LOG.info("Bot is disconnecting from Discord")
        System.exit(0)
    }
}