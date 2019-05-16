package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.DisconnectEvent
import net.dv8tion.jda.core.events.ResumedEvent
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

    private val shardId: Int by lazy {
        logChannel?.jda?.shardInfo?.shardId ?: 0
    }

    private val pendingMessages = LinkedList<String>()
    private var connected = false
    private var disconnectedAt = mutableMapOf<JDA, Long>()

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
        if (logChannel?.jda?.status == JDA.Status.CONNECTED) {
            logChannel?.sendMessage(msg)?.queue()
        } else {
            Bot.LOG.debug("Logging \"$msg\" while disconnected. Queueing for reconnect")
            pendingMessages.add(msg)
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
        log(":e_mail: Received RESUME. Was disconnected for ${Time.format(1, dcTime)}", event.jda)
        if (event.jda.shardInfo?.shardId ?: shardId == shardId) {
            sendQueuedMessages()
        }
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        if (event.jda.shardInfo?.shardId ?: shardId == shardId) {
            this.connected = false
        }
        this.disconnectedAt[event.jda] = event.disconnectTime.toEpochSecond() * 1000
        log(":e_mail: :warning: Disconnected from the websocket at ${SimpleDateFormat(
                "MM-dd-yy HH:mm:ss").format(
                event.disconnectTime.toEpochSecond())} (${event.closeCode})", event.jda)
    }
}