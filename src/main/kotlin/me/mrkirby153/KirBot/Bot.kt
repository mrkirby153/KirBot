package me.mrkirby153.KirBot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.realname.RealnameUpdater
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.localizeTime
import me.mrkirby153.KirBot.utils.readProperties
import me.mrkirby153.KirBot.web.WebApp
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.utils.SimpleLog
import ro.pippo.core.Pippo
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Bot {

    @JvmStatic val LOG = SimpleLog.getLog("KirBot")

    var initialized = false

    val startTime = System.currentTimeMillis()
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val files = BotFiles()

    val properties = files.properties.readProperties()

    val numShards: Int = if (properties.getProperty("shards") == null) 1 else properties.getProperty("shards").toInt()

    val admins: List<String> = files.admins.run { this.readLines() }

    lateinit var webServer: Pippo

    lateinit var shards: Array<Shard>

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
        val startTime = System.currentTimeMillis()
        shards = Array(numShards) { id ->
            LOG.info("Starting shard $id")
            val jda = buildJDA(id, token)

            LOG.info("Shard $id is ready...")

            Shard(id, jda, this)
        }
        val endTime = System.currentTimeMillis()
        LOG.info("\n\n\nSHARDS INITIALIZED! (${localizeTime(((endTime - startTime) / 1000).toInt())})")
        LOG.info("Starting real name updater thread")
        scheduler.scheduleAtFixedRate(RealnameUpdater(), 60, 60, TimeUnit.SECONDS)
        scheduler.scheduleAtFixedRate({
            shards
                    .flatMap { it.guilds }
                    .forEach { Database.updateChannels(it) }
        }, 120, 120, TimeUnit.SECONDS)

        LOG.info("Bot is connecting to discord")

        LOG.info("Updating names")
        shards
                .flatMap { it.guilds }
                .forEach { Database.onJoin(it) }

        webServer = Pippo(WebApp())
        webServer.server.port = properties.getProperty("webserver-port", "5656").toInt()
        webServer.server.settings.host(properties.getProperty("webserver-host", "localhost"))
        webServer.start()
        LOG.info("Web server started on ${webServer.server.settings.host}:${webServer.server.port}")

        HttpUtils.clearCache()

    }

    fun stop() {
        shards.forEach { it.shutdown() }
        LOG.info("Bot is disconnecting from Discord")
        System.exit(0)
    }

    fun buildJDA(id: Int, token: String): JDA {
        return JDABuilder(AccountType.BOT).run {
            setToken(token)
            setAutoReconnect(true)
            if (numShards > 1) {
                useSharding(id, numShards)
                setGame(Game.of("~help | Shard $id of $numShards"))
            } else {
                setGame(Game.of("| ~help"))
            }
            setAudioSendFactory(NativeAudioSendFactory())
            buildBlocking()
        }
    }

    fun getUser(id: String): User? {
        return getShardForUser(id)?.getUserById(id)
    }

    fun getShardForUser(id: String): Shard? {
        return shards.firstOrNull { it.getUserById(id) != null }
    }

    fun getShardForGuild(id: String): Shard? {
        return shards.firstOrNull { it.getGuildById(id) != null }
    }

    fun getGuild(id: String): Guild? {
        return shards
                .firstOrNull { it.getGuildById(id) != null }
                ?.getGuildById(id)
    }

    fun getServerData(guild: Guild): ServerData? = getShardForGuild(guild.id)?.getServerData(guild)
}