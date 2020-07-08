package com.mrkirby153.kirbot.config

import io.mockk.mockk
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DiscordConfiguration {

    @Bean
    fun shardManager(): ShardManager {
        return mockk()
    }
}