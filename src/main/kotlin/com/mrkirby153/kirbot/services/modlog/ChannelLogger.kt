package com.mrkirby153.kirbot.services.modlog

import com.mrkirby153.kirbot.utils.checkPermissions
import com.mrkirby153.kirbot.utils.responseBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.exceptions.RateLimitedException
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager

const val QUIET_PERIOD_DURATION_MS = 60 * 1000
const val SLEEP_PERIOD_MS = 5 * 1000

/**
 * A channel logger for sending events to channels
 */
class ChannelLogger(val shardManager: ShardManager, val guildId: String, val channelId: String,
                    initialSubscriptions: List<LogEvent>) {
    companion object {
        private val log = LogManager.getLogger()
    }


    private val pendingLogMessages = mutableListOf<String>()
    private val subscriptions = mutableSetOf<LogEvent>()

    private var quietPeriod = -1L
    private var sleepUntil = 0L

    init {
        synchronized(subscriptions) {
            subscriptions.addAll(initialSubscriptions)
        }
    }

    /**
     * Submits a [message] for the given [event] to the queue. Returns true if the message was
     * successfully submitted
     */
    fun submit(message: String, event: LogEvent): Boolean {
        synchronized(subscriptions) {
            if (event !in subscriptions) {
                return false;
            }
        }
        synchronized(pendingLogMessages) {
            pendingLogMessages.add(message)
        }
        return true
    }

    /**
     * Updates this channel logger's [subscriptions]
     */
    fun updateSubscriptions(subscriptions: List<LogEvent>) {
        this.subscriptions.addAll(subscriptions)
        this.subscriptions.removeIf { it !in subscriptions}
    }

    /**
     * Processes the pending log messages
     */
    fun log() {
        synchronized(pendingLogMessages) {
            if (pendingLogMessages.isEmpty() || System.currentTimeMillis() < sleepUntil) {
                return
            }
            sleepUntil = -1
            val chan = shardManager.getTextChannelById(channelId) ?: throw InvalidLoggerException(
                    "Channel not found")
            if (!chan.checkPermissions(Permission.MESSAGE_WRITE))
                throw InvalidLoggerException("No permission to send message to channel")

            val events = getNextLogMessage()
            val msgStr = events.joinToString("\n")
            try {
                chan.responseBuilder.sendMessage(msgStr)?.complete(false)
            } catch (e: RateLimitedException) {
                log.debug("Ratelimited. Entering quiet period for the next 60s")
                pendingLogMessages.addAll(0, events.reversed())
                quietPeriod = System.currentTimeMillis() + QUIET_PERIOD_DURATION_MS
            }
            if (quietPeriod != -1L) {
                if (System.currentTimeMillis() > this.quietPeriod) {
                    log.debug("Quiet period expired")
                    quietPeriod = -1
                }
                sleepUntil = System.currentTimeMillis() + SLEEP_PERIOD_MS
            }
        }
    }

    private fun getNextLogMessage(): List<String> {
        var length = 0
        val events = mutableListOf<String>()
        synchronized(pendingLogMessages) {
            while (pendingLogMessages.isNotEmpty()) {
                val msg = pendingLogMessages[0]
                if (msg.length + 1 + length > 2000) {
                    break
                }
                pendingLogMessages.removeAt(0)
                length += msg.length
                events.add(msg)
            }
        }
        return events
    }

    /**
     * Checks if this channel logger has any pending messages
     */
    fun hasPendingMessages() = synchronized(pendingLogMessages) { pendingLogMessages.isNotEmpty() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChannelLogger

        if (channelId != other.channelId) return false

        return true
    }

    override fun hashCode(): Int {
        return channelId.hashCode()
    }


    class InvalidLoggerException(message: String) : Exception(message)
}