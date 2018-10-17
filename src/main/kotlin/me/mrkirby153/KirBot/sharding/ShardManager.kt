package me.mrkirby153.KirBot.sharding

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class ShardManager(val token: String, private val totalShards: Int) {

    val shards = mutableListOf<Shard>()
    private val loadingShards = mutableListOf<Int>()

    var playing: String = ""
        set(game) {
            field = game
            Bot.LOG.debug("Updating playing game to $game")
            if (autoUpdatePresence)
                shards.forEach { updatePresence(it) }
        }
    var onlineStatus: OnlineStatus = OnlineStatus.IDLE
        set(status) {
            field = status
            Bot.LOG.debug("Updating online status to $status")
            if (autoUpdatePresence)
                shards.forEach {
                    updatePresence(it)
                }
        }
    var gameType: Game.GameType = Game.GameType.DEFAULT
        set(game) {
            field = game
            Bot.LOG.debug("Updating game type to $game")
            if (autoUpdatePresence)
                shards.forEach { updatePresence(it) }
        }

    var autoUpdatePresence = true

    fun updatePresence(jda: JDA? = null) {
        if (jda == null) {
            shards.forEach { updatePresence(it) }
        } else {
            val game = if (totalShards > 1) Game.of(gameType,
                    "$playing | Shard ${jda.shardInfo.shardId} of $totalShards") else Game.of(
                    gameType,
                    playing)
            Bot.LOG.debug("Updating presence for $jda to $game ($onlineStatus)")
            jda.presence.setPresence(onlineStatus, game)
        }
    }

    fun addShard(id: Int) {
        if (id > totalShards) {
            throw IllegalArgumentException("Cannot start more shards than the allocated amount")
        }
        loadingShards.add(id)
        Bot.LOG.info("Starting shard ${id + 1}/${this.totalShards}")
        val jda = buildJDA(id)
        jda.awaitReady()
        shards.add(Shard(id, jda, Bot))
        if (totalShards > 1)
            Thread.sleep(5000)
    }

    fun addListener(eventListener: Any) {
        Bot.LOG.debug("Adding event listener ${eventListener.javaClass}")
        shards.forEach { it.addEventListener(eventListener) }
    }

    fun removeListener(eventListener: Any) {
        shards.forEach { it.removeEventListener(eventListener) }
    }

    fun getShardById(id: Int): Shard {
        return shards.first { it.id == id }
    }

    fun shutdown() {
        shards.forEach(Shard::shutdown)
    }

    fun shutdown(id: Int) {
        getShardById(id).shutdown()
    }

    fun getUser(id: String): User? {
        shards.forEach {
            val user = it.getUserById(id)
            if (user != null) {
                return user
            }
        }
        return null
    }

    fun getGuild(id: String): Guild? {
        shards.forEach {
            if (it.getGuildById(id) != null)
                return it.getGuildById(id)
        }
        return null
    }

    fun getShard(guildId: String): Shard? {
        shards.forEach {
            if (it.getGuildById(guildId) != null) {
                return it
            }
        }
        return null
    }

    fun getShard(guild: Guild): Shard? {
        return getShard(guild.id)
    }

    fun isLoading() = this.loadingShards.size > 0

    private fun buildJDA(id: Int = 0) = JDABuilder(AccountType.BOT).run {
        setToken(this@ShardManager.token)
        setAutoReconnect(true)
        setStatus(OnlineStatus.IDLE)
        setBulkDeleteSplittingEnabled(false)
        if (totalShards > 1) {
            useSharding(id, totalShards)
        }
        setGame(Game.playing("Starting up..."))
        if (!System.getProperty("os.name").contains("Mac"))
            setAudioSendFactory(NativeAudioSendFactory())
        addEventListener(object : ListenerAdapter() {
            override fun onReady(event: ReadyEvent?) {
                Bot.LOG.info("Shard $id is ready!")
                loadingShards.remove(id)
                event?.jda?.let {
                    removeEventListener(this);
                    updatePresence(it)
                }
            }
        })
        build()
    }

    fun getTextChannel(id: String): TextChannel? {
        val channels = shards.flatMap { s -> s.guilds.flatMap { it.textChannels } }
        return channels.firstOrNull { it.id == id }
    }
}