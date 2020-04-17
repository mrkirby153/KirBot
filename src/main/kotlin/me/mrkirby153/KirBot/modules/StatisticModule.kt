package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.MessageConcurrencyManager
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.stats.Statistics
import me.mrkirby153.KirBot.utils.getOnlineStats
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.stream.Collectors
import javax.inject.Inject

class StatisticModule @Inject constructor(private val shardManager: ShardManager): Module("stats") {
    override fun onLoad() {
    }


    @Periodic(1)
    fun updateUsers() {
        // Users
        val users = shardManager.shards.flatMap { it.users }.filter { !it.isBot }.stream().collect(
                Collectors.groupingBy(User::getOnlineStats))
        users.forEach { status, users ->
            Statistics.userCount.labels(status.key).set(users.size.toDouble())
        }
        val botCount = shardManager.shards.flatMap { it.users }.filter { it.isBot }.count()
        Statistics.botCount.set(botCount.toDouble())
        Statistics.guilds.set(shardManager.shards.flatMap { it.guilds }.count().toDouble())

        shardManager.shards.forEach { shard ->
            val shardNumber = shard.shardInfo.shardId
            Statistics.websocketPing.labels(shardNumber.toString()).set(shard.gatewayPing.toDouble())
        }
        Statistics.pendingMessageJobs.set(MessageConcurrencyManager.queueSize().toDouble())
        Statistics.pendingMessages.set(MessageConcurrencyManager.messageCount().toDouble())
        Statistics.runningMessageJobs.set(MessageConcurrencyManager.runningJobs().toDouble())
    }


    @Subscribe
    fun messageReceived(event: GuildMessageReceivedEvent) {
        when {
            event.author == event.jda.selfUser -> Statistics.ownMessages.labels(event.guild.id).inc()
            event.author.isBot -> Statistics.botMessages.labels(event.guild.id).inc()
            else -> Statistics.messages.labels(event.guild.id).inc()
        }
    }

}