package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.localizeTime
import java.awt.Color

@Command(name = "stats", clearance = Clearance.USER, description = "Returns some statistics about the robot", category = "Miscellaneous")
class CommandStats : CommandExecutor() {
    override fun execute(context: Context, args: Array<String>) {
        var guilds = 0
        var users = 0

        for(shard in Bot.shards){
            guilds += shard.guilds.size
            users += shard.users.size
        }

        context.send().embed("Bot Statistics") {
            setColor(Color.BLUE)

            field("Servers", true, guilds)
            field("Shard", true, "${shard.id} / ${Bot.numShards}")
            field("Users", true, users)
            val time = System.currentTimeMillis() - Bot.startTime
            field("Uptime", true, localizeTime((time / 1000).toInt()))
        }.rest().queue()
    }
}