package me.mrkirby153.KirBot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.database.DatabaseConnection
import me.mrkirby153.KirBot.database.api.enableApiDebug
import me.mrkirby153.KirBot.database.models.ConnectionFactory
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.error.UncaughtErrorReporter
import me.mrkirby153.KirBot.logger.LogListener
import me.mrkirby153.KirBot.redis.RedisConnector
import me.mrkirby153.KirBot.redis.messaging.MessageDataStore
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.scheduler.Scheduler
import me.mrkirby153.KirBot.seen.SeenStore
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.ShardManager
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.localizeTime
import me.mrkirby153.KirBot.utils.readProperties
import me.mrkirby153.KirBot.utils.redis.RedisConnection
import me.mrkirby153.kcutils.Time
import me.mrkirby153.kcutils.readProperties
import net.dv8tion.jda.core.OnlineStatus
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object Bot {

    @JvmStatic
    val LOG = LoggerFactory.getLogger("KirBot")

    var initialized = false

    val startTime = System.currentTimeMillis()
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val files = BotFiles()

    val properties = files.properties.readProperties()

    val numShards: Int = if (properties.getProperty(
            "shards") == null) 1 else properties.getProperty("shards").toInt()

    val admins: List<String> = files.admins.run { this.readLines() }

    val seenStore = SeenStore()

    val messageDataStore = MessageDataStore()

    val constants = Bot.javaClass.getResourceAsStream("/constants.properties").readProperties()

    val playerManager: AudioPlayerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())
        registerSourceManager(SoundCloudAudioSourceManager())
        registerSourceManager(VimeoAudioSourceManager())
        registerSourceManager(TwitchStreamAudioSourceManager())
        registerSourceManager(BeamAudioSourceManager())
    }

    val debug = properties.getProperty("debug", "false").toBoolean()

    lateinit var redisConnection: RedisConnection

    lateinit var shardManager: ShardManager

    lateinit var database: DatabaseConnection


    fun start(token: String) {
        val startupTime = System.currentTimeMillis()
        if (debug) {
            enableApiDebug()
            (LOG as? Logger)?.let { logger ->
                logger.level = Level.DEBUG
            }
        }
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as? Logger)?.level = Level.valueOf(
                System.getProperty("kirbot.global_log", "INFO"))
        Bot.LOG.info("Starting KirBot ${constants.getProperty("bot-version")}")
        Bot.LOG.debug("\t + Base URL: ${constants.getProperty("bot-base-url")}")

        Thread.setDefaultUncaughtExceptionHandler(UncaughtErrorReporter())

        if (initialized)
            throw IllegalStateException("Bot has already been initialized!")
        initialized = true

        // Attempt database initialization
        LOG.info("Initializing database connection")
        database = DatabaseConnection(properties.getProperty("database-host"),
                properties.getProperty("database-port").toInt(), properties.getProperty("database"),
                properties.getProperty("database-username"),
                properties.getProperty("database-password"))

        Model.factory = object : ConnectionFactory {
            override fun getConnection(): Connection {
                return this@Bot.database.getConnection()
            }
        }

        LOG.info("Initializing Bot ($numShards shards)")
        val startTime = System.currentTimeMillis()

        shardManager = ShardManager(token, numShards)
        shardManager.playing = "Starting up..."
        shardManager.onlineStatus = OnlineStatus.IDLE
        for (i in 0 until numShards) {
            shardManager.addShard(i)
        }

        shardManager.addListener(LogListener())

        val endTime = System.currentTimeMillis()
        LOG.info("\n\n\nSHARDS INITIALIZED! (${localizeTime(
                ((endTime - startTime) / 1000).toInt())})")

        LOG.info("Bot is connecting to discord")

        val guilds = shardManager.shards.flatMap { it.guilds }
        LOG.info("Started syncing of ${guilds.size} guilds")
        val syncTime = measureTimeMillis {
            guilds.forEach {
                LOG.info("Syncing guild ${it.id} (${it.name})")
                KirBotGuild[it].sync()
            }
        }
        LOG.info("Synced ${guilds.size} guilds in ${Time.format(1, syncTime)}")


        val password = Bot.properties.getProperty("redis-password", "")
        val host = Bot.properties.getProperty("redis-host", "localhost")
        val port = Bot.properties.getProperty("redis-port", "6379").toInt()
        val dbNumber = Bot.properties.getProperty("redis-db", "0").toInt()

        this.redisConnection = RedisConnection(host, port,
                if (password.isEmpty()) null else password, dbNumber)

        RedisConnector.listen()

        HttpUtils.clearCache()

        CommandExecutor.loadAll()

        Scheduler.load()

        scheduler.scheduleAtFixedRate(FeedTask(), 0, 1, TimeUnit.HOURS)

        shardManager.onlineStatus = OnlineStatus.ONLINE
        shardManager.playing = properties.getOrDefault("playing-message", "!help").toString()
        LOG.info("Startup completed in ${Time.format(0, System.currentTimeMillis() - startupTime)}")
    }

    fun stop() {
        RedisConnector.running = false
        shardManager.shutdown()
        LOG.info("Bot is disconnecting from Discord")
        System.exit(0)
    }

}