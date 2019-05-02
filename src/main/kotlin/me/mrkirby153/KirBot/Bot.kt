package me.mrkirby153.KirBot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.SoftDeletingModel
import com.mrkirby153.bfs.sql.QueryBuilder
import com.mrkirby153.bfs.sql.elements.Pair
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.mrkirby153.KirBot.command.CommandDocumentationGenerator
import me.mrkirby153.KirBot.database.DatabaseConnection
import me.mrkirby153.KirBot.database.models.guild.DiscordGuild
import me.mrkirby153.KirBot.error.UncaughtErrorReporter
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.ShardManager
import me.mrkirby153.KirBot.stats.Statistics
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.readProperties
import me.mrkirby153.kcutils.Time
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.readProperties
import me.mrkirby153.kcutils.utils.SnowflakeWorker
import net.dv8tion.jda.core.OnlineStatus
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object Bot {

    @JvmStatic
    val LOG = LoggerFactory.getLogger("KirBot")

    var initialized = false

    var state = BotState.UNKNOWN

    val startTime = System.currentTimeMillis()
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(5)

    val files = BotFiles()

    val properties = files.properties.readProperties()

    var numShards: Int = 1

    val constants = Bot.javaClass.getResourceAsStream("/constants.properties").readProperties()

    val gitProperties = Bot.javaClass.getResourceAsStream("/git.properties").readProperties()

    val playerManager: AudioPlayerManager = DefaultAudioPlayerManager().apply {
        AudioSourceManagers.registerRemoteSources(this)
        AudioSourceManagers.registerLocalSource(this)
    }

    val debug = properties.getProperty("debug", "false").toBoolean()

    val idGenerator = SnowflakeWorker(1, 1)

    lateinit var shardManager: ShardManager

    @Deprecated("Deprecated")
    lateinit var database: DatabaseConnection


    fun start(token: String) {
        Statistics.export()
        val startupTime = System.currentTimeMillis()
        state = BotState.INITIALIZING
        if (debug) {
            (LOG as? Logger)?.let { logger ->
                logger.level = Level.DEBUG
            }
            QueryBuilder.logQueries = System.getProperty("kirbot.logQueries",
                    "false")?.toBoolean() ?: false
            if (QueryBuilder.logQueries) {
                LOG.warn("Query logging is enabled. This will generate a lot of console output!")
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

        // Get the number of shards to start with
        numShards = if (properties.getProperty("shards") == null || properties.getProperty(
                        "shards") == "auto") {
            LOG.info("Automatically determining the number of shards to use")
            getNumShards(token)
        } else {
            properties.getProperty("shards").toInt()
        }

        LOG.info("Initializing Bot ($numShards shards)")
        val startTime = System.currentTimeMillis()

        shardManager = ShardManager(token, numShards)
        shardManager.playing = "Starting up..."
        shardManager.onlineStatus = OnlineStatus.IDLE
        shardManager.addListener(AdminControl)
        state = BotState.CONNECTING
        for (i in 0 until numShards) {
            shardManager.addShard(i)
        }

        val endTime = System.currentTimeMillis()
        LOG.info("\n\n\nSHARDS INITIALIZED! (${Time.format(1, endTime - startTime)})")

        state = BotState.LOADING

        // Boot the modules
        ModuleManager.load(true)

        // Remove old guilds
        Bot.LOG.info("Purging old guilds...")
        // Remove guilds whose time has passed
        SoftDeletingModel.trashed(DiscordGuild::class.java).where("deleted_at", "<",
                Timestamp.from(Instant.now().minus(Duration.ofDays(30)))).get().forEach { it.forceDelete() }
        val guildList = shardManager.shards.flatMap { it.guilds }
        Model.query(DiscordGuild::class.java).whereNotIn("id",
                guildList.map { it.id }.toTypedArray()).whereNull("deleted_at").update(
                Pair("deleted_at", Timestamp(System.currentTimeMillis())))


        val guilds = shardManager.shards.flatMap { it.guilds }
        LOG.info("Started syncing of ${guilds.size} guilds")
        val syncTime = measureTimeMillis {
            guilds.forEach {
                LOG.info("Syncing guild ${it.id} (${it.name})")
                KirBotGuild[it].syncSeenUsers()
                KirBotGuild[it].sync()
                KirBotGuild[it].dispatchBackfill()
            }
        }

        LOG.info("Synced ${guilds.size} guilds ${Time.format(1,
                syncTime)}")

        HttpUtils.clearCache()

        scheduler.scheduleAtFixedRate(FeedTask(), 0, 15, TimeUnit.MINUTES)
        ModuleManager.startScheduler()
        Infractions.waitForInfraction()

        // Register listener for nick changes
        SettingsRepository.registerSettingListener("bot_nick") { guild, value ->
            guild.controller.setNickname(guild.selfMember, value).queue()
        }

        shardManager.onlineStatus = OnlineStatus.ONLINE
        shardManager.playing = properties.getOrDefault("playing-message", "!help").toString()
        LOG.info("Startup completed in ${Time.format(0, System.currentTimeMillis() - startupTime)}")
        val memberSet = mutableSetOf<String>()
        Bot.shardManager.shards.flatMap { it.guilds }.flatMap { it.members }.forEach {
            if (it.user.id !in memberSet)
                memberSet.add(it.user.id)
        }
        val guildCount = shardManager.shards.flatMap { it.guilds }.count()
        AdminControl.sendQueuedMessages()
        AdminControl.log("Bot startup complete in ${Time.formatLong(
                System.currentTimeMillis() - startTime).toLowerCase()}. On $guildCount guilds with ${memberSet.size} users")

        CommandDocumentationGenerator.generate(files.data.child("commands.md"))
        state = BotState.RUNNING
    }

    fun stop() {
        state = BotState.SHUTTING_DOWN
        AdminControl.log("Bot shutting down...")
        shardManager.shutdown()
        ModuleManager.loadedModules.forEach { it.unload(true) }
        LOG.info("Bot is disconnecting from Discord")
        state = BotState.SHUT_DOWN
        System.exit(0)
    }

    private fun getNumShards(token: String): Int {
        LOG.debug("Asking Discord for the number of shards to use...")
        val request = Request.Builder().apply {
            url("https://discordapp.com/api/v6/gateway/bot")
            header("Authorization", "Bot $token")
        }.build()
        val response = HttpUtils.CLIENT.newCall(request).execute()
        if (response.code() != 200)
            throw RuntimeException(
                    "Received non-success code (${response.code()}) from Discord, aborting")
        val body = response.body()?.string()
        if (body.isNullOrEmpty()) {
            throw RuntimeException(
                    "Could not determine the number of shards. Must be specified manually")
        }
        LOG.debug("Received body $body")
        val json = JSONObject(JSONTokener(body))
        val shards = json.getInt("shards")
        LOG.debug("Discord returned $shards shards.")
        return shards
    }

}