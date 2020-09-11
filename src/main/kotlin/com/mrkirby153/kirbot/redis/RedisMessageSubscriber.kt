package com.mrkirby153.kirbot.redis

import org.apache.logging.log4j.LogManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener

class RedisMessageSubscriber(private val eventPublisher: ApplicationEventPublisher,
                             private val chanPrefix: String) :
        MessageListener {

    private val log = LogManager.getLogger()

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val chan = String(message.channel)
        if (pattern?.contentEquals("$chanPrefix:*".toByteArray()) == true) {
            val key = chan.split(":")
            if (key.size != 2) {
                log.warn("Received message on channel $chan which is not valid.")
                return
            }
            log.debug("Received message ${convertDisplayBody(message.body)} with key ${key[1]}")
            eventPublisher.publishEvent(RedisMessageEvent(key[1], message.body))
        } else {
            log.warn("Received message that does not match the pattern $chanPrefix:*")
        }
    }

    private fun convertDisplayBody(body: ByteArray) = buildString {
        append("[")
        append(body.joinToString(", "))
        append("]")
        try {
            val str = String(body, Charsets.UTF_8)
            append(" ($str)")
        } catch (e: Throwable) {
            append(" (<<String Conversion Failed>>)")
        }
    }
}