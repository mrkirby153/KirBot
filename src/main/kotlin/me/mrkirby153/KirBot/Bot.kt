package me.mrkirby153.KirBot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import co.aikar.idb.DB
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import me.mrkirby153.KirBot.database.DatabaseConnection
import me.mrkirby153.KirBot.error.UncaughtErrorReporter
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.seen.SeenStore
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.ShardManager
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.localizeTime
import me.mrkirby153.KirBot.utils.readProperties
import me.mrkirby153.kcutils.Time
import me.mrkirby153.kcutils.readProperties
import net.dv8tion.jda.core.OnlineStatus
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object Bot {

    @JvmStatic
    val LOG = LoggerFactory.getLogger("KirBot")

    var initialized = false

    val startTime = System.currentTimeMillis()
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    val files = BotFiles()

    val properties = files.properties.readProperties()

    var numShards: Int = 1

    val admins: List<String> = files.admins.run { this.readLines() }

    val seenStore = SeenStore()

    val constants = Bot.javaClass.getResourceAsStream("/constants.properties").readProperties()

    val playerManager: AudioPlayerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())
        registerSourceManager(SoundCloudAudioSourceManager())
        registerSourceManager(VimeoAudioSourceManager())
        registerSourceManager(TwitchStreamAudioSourceManager())
        registerSourceManager(BeamAudioSourceManager())
    }

    val debug = properties.getProperty("debug", "false").toBoolean()

    lateinit var shardManager: ShardManager

    @Deprecated("Deprecated")
    lateinit var database: DatabaseConnection


    fun start(token: String) {
        val startupTime = System.currentTimeMillis()
        if (debug) {
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
        for (i in 0 until numShards) {
            shardManager.addShard(i)
        }

        val endTime = System.currentTimeMillis()
        LOG.info("\n\n\nSHARDS INITIALIZED! (${localizeTime(
                ((endTime - startTime) / 1000).toInt())})")

        // Boot the modules
        ModuleManager.loadModules(true)

        val guilds = shardManager.shards.flatMap { it.guilds }
        LOG.info("Started syncing of ${guilds.size} guilds")
        val syncTime = measureTimeMillis {
            guilds.forEach {
                LOG.info("Syncing guild ${it.id} (${it.name})")
                KirBotGuild[it].sync()
                KirBotGuild[it].syncSeenUsers(true)
            }
        }
        // Remove old guilds
        Bot.LOG.info("Purging old guilds...")
        val guildList = shardManager.shards.flatMap { it.guilds }
        val sql = "DELETE FROM `server_settings` WHERE `id` NOT IN (${guildList.joinToString(
                ",") { "'${it.id}'" }})"
        val deleted = DB.executeUpdate(sql)

        LOG.info("Synced ${guilds.size} guilds and removed $deleted guilds in ${Time.format(1,
                syncTime)}")

        HttpUtils.clearCache()

        scheduler.scheduleAtFixedRate(FeedTask(), 0, 1, TimeUnit.HOURS)

        shardManager.onlineStatus = OnlineStatus.ONLINE
        shardManager.playing = properties.getOrDefault("playing-message", "!help").toString()
        LOG.info("Startup completed in ${Time.format(0, System.currentTimeMillis() - startupTime)}")
        val memberSet = mutableSetOf<String>()
        Bot.shardManager.shards.flatMap { it.guilds }.flatMap { it.members }.forEach {
            if(it.user.id !in memberSet)
                memberSet.add(it.user.id)
        }
        val guildCount = shardManager.shards.flatMap { it.guilds }.count()
        AdminControl.log("Bot startup complete in ${Time.formatLong(
                System.currentTimeMillis() - startTime).toLowerCase()}. On $guildCount guilds with ${memberSet.size} users")
    }

    fun stop() {
        AdminControl.log("Bot shutting down...")
        shardManager.shutdown()
        ModuleManager.loadedModules.forEach { it.unload(true) }
        LOG.info("Bot is disconnecting from Discord")
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