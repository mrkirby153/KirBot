package com.mrkirby153.kirbot.config

import org.apache.logging.log4j.LogManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import redis.embedded.RedisServer
import java.util.Random
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

const val MIN_PORT = 49152
const val MAX_PORT = 65535

@TestConfiguration
class TestRedisConfiguration {

    final val redisPort = Random().nextInt((MAX_PORT - MIN_PORT) + 1) + MIN_PORT

    val redisServer: RedisServer = RedisServer(redisPort)

    @Bean
    @Profile("test")
    fun redisConnectionFactory(): RedisConnectionFactory = LettuceConnectionFactory(
            RedisStandaloneConfiguration("localhost", redisPort))

    @PostConstruct
    fun postConstruct() {
        log.info("Starting embedded redis server on port ${redisServer.ports()}")
        redisServer.start()
    }

    @PreDestroy
    fun preDestroy() {
        log.info("Shutting down embedded redis server")
        redisServer.stop()
    }

    companion object {
        val log = LogManager.getLogger()
    }
}