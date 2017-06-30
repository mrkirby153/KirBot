package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.localizeTime
import java.awt.Color

class CommandStats : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        var guilds = 0
        var users = 0

        for (shard in Bot.shards) {
            guilds += shard.guilds.size
            users += shard.users.size
        }

        context.send().embed("Bot Statistics") {
            setColor(Color.BLUE)

            field("Servers", true, guilds)
            if (Bot.numShards != 1)
                field("Shard", true, "${context.shard.id} / ${Bot.numShards}")
            field("Users", true, users)
            val time = System.currentTimeMillis() - Bot.startTime
            field("Uptime", true, localizeTime((time / 1000).toInt()))
        }.rest().queue()
    }
}