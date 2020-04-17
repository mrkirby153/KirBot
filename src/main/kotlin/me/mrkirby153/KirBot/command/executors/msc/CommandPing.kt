package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.globalAdmin
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.sharding.ShardManager
import javax.inject.Inject
import kotlin.math.roundToLong


class CommandPing @Inject constructor(private val shardManager: ShardManager){

    @Command(name = "ping", category = CommandCategory.MISCELLANEOUS)
    @CommandDescription("Check the bot's ping")
    fun execute(context: Context, cmdContext: CommandContext) {
        val start = System.currentTimeMillis()
        context.channel.sendTyping().queue {
            val stop = System.currentTimeMillis()
            val adminMsg = "Ping: `${Time.format(1, stop - start)}`\nHeartbeat: `${Time.format(1,
                    context.jda.gatewayPing)}` \nAverage across all shards (${shardManager.shards.size}): `${Time.format(
                    1, shardManager.averageGatewayPing.roundToLong())}`"
            val msg = "Pong! `${Time.format(1, stop - start)}`"
            context.channel.sendMessage(if(context.author.globalAdmin) adminMsg else msg).queue()
        }
    }
}