package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.database.Database
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class ShardListener(val shard: Shard, val bot: Bot): ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if(event!!.author == shard.selfUser)
            return
        val serverData = shard.getServerData(event.guild)

        CommandManager.call(event, serverData, shard, event.guild)
    }

    override fun onGuildLeave(event: GuildLeaveEvent?) {
        Database.onLeave(event!!.guild)
    }

    override fun onGuildJoin(event: GuildJoinEvent?) {
        Database.onJoin(event!!.guild)
    }
}