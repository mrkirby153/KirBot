package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.globalAdmin
import me.mrkirby153.kcutils.Time

@Command(name = "ping")
class CommandPing : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val start = System.currentTimeMillis()
        context.channel.sendTyping().queue {
            val stop = System.currentTimeMillis()
            val avgHeartbeat = Bot.shardManager.shards.map { it.ping }.sum() / Bot.shardManager.shards.size.toDouble()
            val adminMsg = "Ping: `${Time.format(1, stop - start)}`\nHeartbeat: `${Time.format(1,
                    context.shard.ping)}` \nAverage across all shards (${Bot.shardManager.shards.size}): `${Time.format(
                    1, avgHeartbeat.toLong())}`"
            val msg = "Pong! `${Time.format(1, stop - start)}`"
            context.channel.sendMessage(if(context.author.globalAdmin) adminMsg else msg).queue()
        }
    }
}