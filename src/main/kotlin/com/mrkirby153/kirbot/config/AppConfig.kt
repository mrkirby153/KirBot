package com.mrkirby153.kirbot.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

@Configuration
class AppConfig(@Value("\${redis.host:localhost}") private val redisHost: String,
                @Value("\${redis.port:6379}") private val redisPort: Int) {

    @Bean
    fun redisConnectionFactory() = LettuceConnectionFactory(
            RedisStandaloneConfiguration(redisHost, redisPort))
}