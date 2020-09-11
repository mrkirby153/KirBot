package com.mrkirby153.kirbot.config

import com.mrkirby153.kirbot.redis.RedisMessageSubscriber
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter

@Configuration
class AppConfig(@Value("\${redis.host:localhost}") private val redisHost: String,
                @Value("\${redis.port:6379}") private val redisPort: Int,
                @Value("\${redis.topic-prefix:kirbot}") private val redisTopic: String) {

    private val log = LogManager.getLogger()

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory = LettuceConnectionFactory(
            RedisStandaloneConfiguration(redisHost, redisPort))

    @Bean
    fun redisContainer(factory: RedisConnectionFactory,
                       listener: MessageListenerAdapter) = RedisMessageListenerContainer().apply {
        log.info("Listening on topic $redisTopic:*")
        setConnectionFactory(factory)
        addMessageListener(listener, PatternTopic.of("$redisTopic:*"))
    }

    @Bean
    fun messageListener(eventPublisher: ApplicationEventPublisher) = MessageListenerAdapter(
            RedisMessageSubscriber(eventPublisher, redisTopic))
}