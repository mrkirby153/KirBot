package me.mrkirby153.KirBot.modules

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.DisconnectEvent
import net.dv8tion.jda.api.events.ResumedEvent
import net.dv8tion.jda.api.requests.CloseCode
import java.text.SimpleDateFormat
import java.util.LinkedList

object AdminControl {

    val logChannel: TextChannel?
        get() {
            Bot.shardManager.shards.flatMap { it.guilds }.forEach { guild ->
                if (Bot.properties.getProperty("control-channel") == null)
                    return null
                if (guild.getTextChannelById(Bot.properties.getProperty("control-channel")) != null)
                    return guild.getTextChannelById(Bot.properties.getProperty("control-channel"))
            }
            return null
        }
    private val webhookUrl = Bot.properties.getProperty("webhook-url")

    private val webhookClient: WebhookClient by lazy {
        WebhookClientBuilder(webhookUrl).build()
    }

    private val shardId: Int by lazy {
        logChannel?.jda?.shardInfo?.shardId ?: 0
    }

    private val pendingMessages = LinkedList<String>()
    private var connected = false
    private var disconnectedAt = mutableMapOf<JDA, Long>()

    private var disconnectCode = mutableMapOf<JDA, CloseCode?>()


    fun sendWebhookMessage(message: String) {
        this.webhookClient.send(message)
    }

    fun log(message: String, jda: JDA? = null) {
        val msg = buildString {
            append("[`")
            append(SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis()))
            append("`]")
            if (jda != null && jda.shardInfo != null) {
                append(" (${jda.shardInfo.shardId}/${jda.shardInfo.shardTotal})")
            }
            append(" $message")
        }

        if(this.webhookUrl != null) {
            sendWebhookMessage(msg)
        } else {
            if (logChannel?.jda?.status == JDA.Status.CONNECTED) {
                logChannel?.sendMessage(msg)?.queue()
            } else {
                Bot.LOG.debug("Logging \"$msg\" while disconnected. Queueing for reconnect")
                pendingMessages.add(msg)
            }
        }
    }

    fun sendQueuedMessages() {
        Bot.LOG.debug("Control channel shard ready, replaying queued messages")
        while (pendingMessages.isNotEmpty()) {
            if (logChannel == null) {
                pendingMessages.clear()
                continue
            }
            logChannel?.sendMessage(buildString {
                while (pendingMessages.isNotEmpty() && pendingMessages.peek().length + 1 + length < 2000) {
                    appendln(pendingMessages.pop())
                }
            })?.queue()
        }
        this.connected = true
    }

    @Subscribe
     fun onResume(event: ResumedEvent) {
        val dcTime = System.currentTimeMillis() - (this.disconnectedAt.remove(event.jda)
                ?: System.currentTimeMillis())
        val closeCode = this.disconnectCode.remove(event.jda)
        if(closeCode != CloseCode.RECONNECT || dcTime > 5000) {
            log(":e_mail: Received RESUME. Was disconnected for ${Time.format(1, dcTime)}",
                    event.jda)
            if (event.jda.shardInfo.shardId == shardId) {
                sendQueuedMessages()
            }
        }
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        if (event.jda.shardInfo.shardId == shardId) {
            this.connected = false
        }
        this.disconnectedAt[event.jda] = event.timeDisconnected.toEpochSecond() * 1000
        this.disconnectCode[event.jda] = event.closeCode
        if(event.closeCode != CloseCode.RECONNECT) {
            log(":e_mail: :warning: Disconnected from the websocket at ${SimpleDateFormat(
                    "MM-dd-yy HH:mm:ss").format(
                    event.timeDisconnected.toEpochSecond() * 1000)} (${event.closeCode})",
                    event.jda)
        }
    }
}