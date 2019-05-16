package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.globalAdmin
import me.mrkirby153.kcutils.Time
import kotlin.math.roundToLong


class CommandPing {

    @Command(name = "ping", category = CommandCategory.MISCELLANEOUS)
    @CommandDescription("Check the bot's ping")
    fun execute(context: Context, cmdContext: CommandContext) {
        val start = System.currentTimeMillis()
        context.channel.sendTyping().queue {
            val stop = System.currentTimeMillis()
            val adminMsg = "Ping: `${Time.format(1, stop - start)}`\nHeartbeat: `${Time.format(1,
                    context.jda.ping)}` \nAverage across all shards (${Bot.shardManager.shards.size}): `${Time.format(
                    1, Bot.shardManager.averagePing.roundToLong())}`"
            val msg = "Pong! `${Time.format(1, stop - start)}`"
            context.channel.sendMessage(if(context.author.globalAdmin) adminMsg else msg).queue()
        }
    }
}