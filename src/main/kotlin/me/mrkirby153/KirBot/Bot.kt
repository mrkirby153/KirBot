package me.mrkirby153.KirBot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import io.sentry.Sentry
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.api.ApiRequestProcessor
import me.mrkirby153.KirBot.error.UncaughtErrorReporter
import me.mrkirby153.KirBot.redis.RedisConnector
import me.mrkirby153.KirBot.seen.SeenStore
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.localizeTime
import me.mrkirby153.KirBot.utils.readProperties
import me.mrkirby153.KirBot.utils.sync
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.requests.SessionReconnectQueue
import net.dv8tion.jda.core.utils.SimpleLog
import java.util.concurrent.Executors

object Bot {

    @JvmStatic val LOG = SimpleLog.getLog("KirBot")

    var initialized = false

    val startTime = System.currentTimeMillis()
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val files = BotFiles()

    val properties = files.properties.readProperties()

    val numShards: Int = if (properties.getProperty("shards") == null) 1 else properties.getProperty("shards").toInt()

    val admins: List<String> = files.admins.run { this.readLines() }

    val seenStore = SeenStore()

    lateinit var shards: Array<Shard>

    private val loadingShards = mutableListOf<Int>()

    val playerManager: AudioPlayerManager = DefaultAudioPlayerManager().apply {
        registerSourceManager(YoutubeAudioSourceManager())
        registerSourceManager(SoundCloudAudioSourceManager())
        registerSourceManager(VimeoAudioSourceManager())
        registerSourceManager(TwitchStreamAudioSourceManager())
        registerSourceManager(BeamAudioSourceManager())
    }

    val debug = properties.getProperty("debug", "false").toBoolean()


    fun start(token: String) {
        // Start up sentry
        Sentry.init()
        Bot.LOG.debug("Starting Sentry")

        Thread.setDefaultUncaughtExceptionHandler(UncaughtErrorReporter())

        if (debug) {
            ApiRequestProcessor.debug()
            LOG.level = SimpleLog.Level.DEBUG
        }
        if (initialized)
            throw IllegalStateException("Bot has already been initialized!")
        initialized = true
        LOG.info("Initializing Bot ($numShards shards)")
        val startTime = System.currentTimeMillis()
        shards = Array(numShards) { id ->
            loadingShards.add(id)
            LOG.info("Starting shard $id (${id + 1}/$numShards)")
            val jda = buildJDA(id, token)
            if(numShards > 1){
                Thread.sleep(5000)
            }
            Shard(id, jda, this)
        }
        LOG.info("Waiting for shards to connect.....")
        while(loadingShards.size > 0) {
            Thread.sleep(150)
        }
        val endTime = System.currentTimeMillis()
        LOG.info("\n\n\nSHARDS INITIALIZED! (${localizeTime(((endTime - startTime) / 1000).toInt())})")

        LOG.info("Bot is connecting to discord")

        LOG.info("Updating channels")
        shards
                .flatMap { it.guilds }
                .forEach { it.sync() }

        RedisConnector.listen()

        HttpUtils.clearCache()

        // Mark all shards as online
        shards.forEach { it.presence.status = OnlineStatus.ONLINE }

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
            setStatus(OnlineStatus.IDLE)
            setBulkDeleteSplittingEnabled(false)
            if (numShards > 1) {
                useSharding(id, numShards)
                setGame(Game.of("~help | Shard $id of $numShards"))
            } else {
                setGame(Game.of("| ~help"))
            }
            setAudioSendFactory(NativeAudioSendFactory())
            addEventListener(object: ListenerAdapter(){
                override fun onReady(event: ReadyEvent) {
                    LOG.info("Shard $id is ready!")
                    loadingShards.remove(id)
                }
            })
            setReconnectQueue(SessionReconnectQueue())
            buildAsync()
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