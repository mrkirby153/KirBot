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
            color = Color.BLUE
            fields {
                field {
                    title = "Servers"
                    inline = true
                    description = guilds.toString()
                }
                if(Bot.numShards != -1)
                    field {
                        title = "Shard"
                        inline = true
                        description  = "${context.shard.id} / ${Bot.numShards}"
                    }
                field {
                    title = "Users"
                    inline = true
                    description = users.toString()
                }
                field {
                    title = "Uptime"
                    inline = true
                    description = localizeTime(((System.currentTimeMillis() - Bot.startTime) / 1000).toInt())
                }
            }
        }.rest().queue()
    }
}