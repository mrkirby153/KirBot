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
    val messages = Counter.build().name("messages_sent").help(
            "How many messages were sent").labelNames("guild").register()
    val ownMessages = Counter.build().name("own_messages").help(
            "How many of our own messages were sent").register()
    val botMessages = Counter.build().name("bot_messages").help(
            "How many messages by bots we have seen").register()

    val userCount = Gauge.build().name("bot_users").help("How many users we've seen").register()
    val botCount = Gauge.build().name("bot_bots").help("How many bots we've seen").register()

    val commandDuration = Histogram.build().name("bot_command_timing").help(
            "How long commands take to process").labelNames("command").register()
    val eventDuration = Histogram.build().name("bot_event_duration").help(
            "How long events take to process").labelNames("event").register()
    val eventType = Counter.build().name("bot_events").help(
            "The different types of events recevied").labelNames("type").register()

    init {
        DefaultExports.initialize()
    }

    fun export() {
        val port = Bot.properties.getProperty("prom-port") ?: return
        Bot.LOG.info("Starting prometheus server on $port")
        server = HTTPServer(port.toInt(), true)
    }
}