package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.stats.Statistics
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent

class StatisticModule : Module("stats") {
    override fun onLoad() {
    }


    @Periodic(1)
    fun updateUsers() {
        // Users
        val userCount = Bot.shardManager.shards.flatMap { it.users }.filter { !it.isBot }.count()
        Statistics.userCount.set(userCount.toDouble())
        val botCount = Bot.shardManager.shards.flatMap { it.users }.filter { it.isBot }.count()
        Statistics.botCount.set(botCount.toDouble())
    }


    @Subscribe
    fun messageReceived(event: GuildMessageReceivedEvent) {
        when {
            event.author == event.jda.selfUser -> Statistics.ownMessages.inc()
            event.author.isBot -> Statistics.botMessages.inc()
            else -> Statistics.messages.labels(event.guild.id).inc()
        }
    }

}