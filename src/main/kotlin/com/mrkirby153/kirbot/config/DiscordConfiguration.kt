package com.mrkirby153.kirbot.config

import com.mrkirby153.kirbot.services.JdaEventService
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for connecting to Discord
 */
@Configuration
class DiscordConfiguration(@Value("\${bot.token}") private val token: String,
                           @Value("\${bot.shard.count:}") private val shardCount: String,
                           @Value("\${bot.shard.min:}") private val shardMin: String,
                           @Value("\${bot.shard.max:}") private val shardMax: String,
                           private val eventService: JdaEventService) {

    private val log = LoggerFactory.getLogger(DiscordConfiguration::class.java)

    @Bean
    fun shardManager(): ShardManager {
        log.info("Connecting to discord")
        // TODO 7/5/20 Figure out the shards that we need to use dynamically from k8s
        val shardManager = DefaultShardManagerBuilder.createDefault(token).apply {
            if (shardCount.isNotBlank()) {
                setShardsTotal(shardCount.toInt())
                log.info("Using manually specified shard count: $shardCount")
            }
            if (shardMin.isNotBlank()) {
                log.info("Connecting shards $shardMin to $shardMax")
                setShards(shardMin.toInt(), shardMax.toInt())
            }
            addEventListeners(eventService)
        }.build()
        shardManager.addEventListener(ReadyListener())
        return shardManager
    }

    private class ReadyListener : ListenerAdapter() {

        private val log = LoggerFactory.getLogger(ReadyListener::class.java)

        override fun onReady(event: ReadyEvent) {
            log.info("Shard ${event.jda.shardInfo.shardString} is ready")
            val shardManager = event.jda.shardManager ?: return
            val connectingShards = shardManager.shards.filter { it.status != JDA.Status.CONNECTED && it.shardInfo.shardId != event.jda.shardInfo.shardId }
            if (connectingShards.isEmpty()) {
                log.info("All shards have connected")
                log.info("On ${shardManager.guilds} guilds")
            }
        }
    }
}