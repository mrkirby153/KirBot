package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.services.ArchiveService
import me.mrkirby153.kcutils.Time
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.net.URL
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Service
class ArchiveManager(
        private val redisTemplate: RedisTemplate<String, String>,
        @Value("\${archive.url}") private val archiveUrlTemplate: String
) : ArchiveService {

    private val log = LogManager.getLogger()

    override fun uploadToArchive(text: String, ttl: Long): CompletableFuture<URL> {
        return CompletableFuture.supplyAsync {
            val key = UUID.randomUUID().toString()
            log.debug("Archive created $key (Expires in ${
                Time.formatLong(ttl * 1000, Time.TimeUnit.SECONDS)
            }")
            redisTemplate.opsForValue().set("archive:$key", text, ttl, TimeUnit.SECONDS)
            URL(String.format(archiveUrlTemplate, key));
        }
    }
}