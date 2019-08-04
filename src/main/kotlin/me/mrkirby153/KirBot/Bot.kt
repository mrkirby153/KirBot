package me.mrkirby153.KirBot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.SoftDeletingModel
import com.mrkirby153.bfs.sql.QueryBuilder
import com.mrkirby153.bfs.sql.elements.Pair
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.mrkirby153.KirBot.command.CommandDocumentationGenerator
import me.mrkirby153.KirBot.database.models.guild.DiscordGuild
import me.mrkirby153.KirBot.error.UncaughtErrorReporter
import me.mrkirby153.KirBot.event.PriorityEventManager
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.listener.ShardListener
import me.mrkirby153.KirBot.listener.WaitUtilsListener
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.stats.Statistics
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.readProperties
import me.mrkirby153.kcutils.Time
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.readProperties
import me.mrkirby153.kcutils.utils.SnowflakeWorker
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.events.ReadyEvent
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.text.SimpleDateFormat
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


    fun start(token: String) {
        Statistics.export()
        val startupTime = System.currentTimeMillis()
        state = BotState.INITIALIZING
        configureLogging()
        Bot.LOG.info("Starting KirBot ${constants.getProperty("bot-version")}")
        Bot.LOG.debug("\t + Base URL: ${constants.getProperty("bot-base-url")}")

        Thread.setDefaultUncaughtExceptionHandler(UncaughtErrorReporter())

        if (initialized)
            throw IllegalStateException("Bot has already been initialized!")
        initialized = true

        // Get the number of shards to start with
        numShards = if (properties.getProperty("shards") == null || properties.getProperty(
                        "shards") == "auto") {
            -1
        } else {
            properties.getProperty("shards").toInt()
        }

        LOG.info("Initializing Bot ($numShards shards)")
        val startTime = System.currentTimeMillis()

        state = BotState.CONNECTING

        shardManager = DefaultShardManagerBuilder(token).apply {
            addEventListeners(AdminControl, ShardListener())
            setEventManagerProvider { PriorityEventManager() }
            setStatus(OnlineStatus.IDLE)
            setShardsTotal(numShards)
            setAutoReconnect(true)
            setBulkDeleteSplittingEnabled(false)
            setGame(Game.playing("Starting up..."))
            if (!System.getProperty("os.name").contains("Mac"))
                setAudioSendFactory(NativeAudioSendFactory())
        }.build()

        Bot.LOG.info("Waiting for shards to start..")

        while (shardManager.shards.firstOrNull { it.status != JDA.Status.CONNECTED } != null) {
            shardManager.shards.first { it.status != JDA.Status.CONNECTED }.awaitReady()
        }

        val endTime = System.currentTimeMillis()
        LOG.info("\n\n\nSHARDS INITIALIZED! (${Time.format(1, endTime - startTime)})")

        state = BotState.LOADING

        // Boot the modules
        ModuleManager.load(true)

        // Remove old guilds
        purgeSoftDeletedGuilds()


        val guilds = shardManager.shards.flatMap { it.guilds }
        LOG.info("Started syncing of ${guilds.size} guilds")
        val syncTime = measureTimeMillis {
            guilds.forEach {
                LOG.info("Syncing guild ${it.id} (${it.name})")
                KirBotGuild[it].syncSeenUsers()
                KirBotGuild[it].sync()
//                KirBotGuild[it].dispatchBackfill()
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

        shardManager.setStatus(OnlineStatus.ONLINE)
        shardManager.setGame(null)
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
        shardManager.addEventListener(object {
            @Subscribe
            fun onReady(event: ReadyEvent) {
                Bot.LOG.info(
                        "Shard ${event.jda.shardInfo.shardId} is ready, syncing all guilds (${event.jda.guilds.size})")
                event.jda.guilds.forEach {
                    KirBotGuild[it].syncSeenUsers()
                    KirBotGuild[it].sync()
                }
            }
        })
        shardManager.addEventListener(WaitUtilsListener())
    }

    private fun purgeSoftDeletedGuilds() {
        LOG.info("Purging old guilds...")
        // Remove guilds whose time has passed
        val threshold = Instant.now().minus(Duration.ofDays(30))
        LOG.info("Removing guilds that we left before ${SimpleDateFormat(
                "MM-dd-yy HH:mm:ss").format(threshold.toEpochMilli())}")
        val guilds = SoftDeletingModel.trashed(DiscordGuild::class.java).where("deleted_at", "<",
                Timestamp.from(threshold)).get()
        LOG.info("${guilds.size} guilds being purged")
        guilds.forEach { it.forceDelete() }
        val guildList = shardManager.shards.flatMap { it.guilds }
        Model.query(DiscordGuild::class.java).whereNotIn("id",
                guildList.map { it.id }.toTypedArray()).whereNull("deleted_at").update(
                Pair("deleted_at", Timestamp(System.currentTimeMillis())))
    }

    private fun configureLogging() {
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