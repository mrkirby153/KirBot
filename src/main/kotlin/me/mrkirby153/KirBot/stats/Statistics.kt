package me.mrkirby153.KirBot.stats

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import me.mrkirby153.KirBot.Bot

object Statistics {

    private lateinit var server: HTTPServer

    val commandsRan = Counter.build().name("commands_ran").help(
            "How many commands were ran").labelNames("command_name", "guild").register()
    val messages = Counter.build().name("messages_received").help(
            "How many messages were received").labelNames("guild").register()
    val ownMessages = Counter.build().name("own_messages").labelNames("guild").help(
            "How many of our own messages were sent").register()
    val botMessages = Counter.build().name("bot_messages").labelNames("guild").help(
            "How many messages by bots we have seen").register()

    val userCount = Gauge.build().name("bot_users").labelNames("status").help(
            "How many users we've seen").register()
    val guilds = Gauge.build().name("bot_guilds").help("Amount of guilds the bot is in").register()

    val botCount = Gauge.build().name("bot_bots").help("How many bots we've seen").register()
    val commandDuration = Histogram.build().name("bot_command_timing").help(
            "How long commands take to process").labelNames("command").register()
    val eventDuration = Histogram.build().name("bot_event_duration").help(
            "How long events take to process").labelNames("event").register()
    val eventType = Counter.build().name("bot_events").help(
            "The different types of events recevied").labelNames("type").register()

    val websocketPing = Gauge.build().name("websocket_ping").help(
            "Current websocket ping").labelNames("shard").register()

    val pendingMessageJobs = Gauge.build().name("pending_message_jobs").help("Jobs waiting to be committed to the database").register()
    val pendingMessages = Gauge.build().name("pending_messages").help("Messages waiting to be committed to the database").register()
    val runningMessageJobs = Gauge.build().name("running_message_jobs").help("Message jobs that are running").register()

    init {
        DefaultExports.initialize()
    }

    fun export() {
        val port = Bot.properties.getProperty("prom-port") ?: return
        Bot.LOG.info("Starting prometheus server on $port")
        server = HTTPServer(port.toInt(), true)
    }
}