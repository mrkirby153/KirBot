package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.KirBotGuild
import net.dv8tion.jda.api.exceptions.RateLimitedException
import java.util.Arrays
import java.util.LinkedList

class ChannelLogger(val guild: KirBotGuild, val channel: String) {
    private val subscriptions = mutableSetOf<LogEvent>()

    private val logQueue = LinkedList<LogManager.LogMessage>()

    private var quietPeriod = -1L
    private var sleepUntil = 0L

    fun setSubscriptions(vararg events: LogEvent) {
        subscriptions.clear()
        subscriptions.addAll(events)
        Bot.LOG.debug("$channel on $guild subscribed to ${Arrays.toString(events)}")
    }

    fun submitEvent(message: LogManager.LogMessage) {
        if (message.event !in subscriptions)
            return // Drop the event, we're not subbed to it
        this.logQueue.add(message)
    }

    fun log() {
        if (logQueue.isEmpty() || System.currentTimeMillis() < sleepUntil)
            return
        sleepUntil = -1

        val targetChannel = this.guild.getTextChannelById(this.channel)
        if (targetChannel == null) {
            Bot.LOG.warn("Guild ${this.guild.id} has outdated log channels")
            guild.logManager.reloadLogChannels()
            return
        }
        val msg = getNextMessage()

        try {
            Bot.LOG.debug("Attempting to send $msg")
            if (targetChannel.canTalk())
                targetChannel.sendMessage(buildString(msg)).complete(false)
        } catch (e: RateLimitedException) {
            Bot.LOG.debug("Ratelimited. Pushing events back on the list")
            msg.reversed().forEach { this.logQueue.push(it) }
            quietPeriod = System.currentTimeMillis() + 60000
        }
        if (quietPeriod != -1L) {
            if (System.currentTimeMillis() > this.quietPeriod)
                quietPeriod = -1
            sleepUntil = System.currentTimeMillis() + 5000
        }
    }

    private fun getNextMessage(): List<LogManager.LogMessage> {
        var charCount = 0
        val evts = mutableListOf<LogManager.LogMessage>()
        while (this.logQueue.isNotEmpty()) {
            val i = logQueue.peek()
            if (i.message.length + 1 + charCount > 2000)
                break
            val msg = logQueue.pop()
            charCount += msg.message.length
            evts.add(msg)
        }
        return evts
    }

    private fun buildString(events: List<LogManager.LogMessage>) = buildString {
        events.forEach {
            appendln(it.message)
        }
    }
}