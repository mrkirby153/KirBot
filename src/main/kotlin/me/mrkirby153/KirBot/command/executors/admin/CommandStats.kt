package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.globalAdmin
import me.mrkirby153.KirBot.utils.localizeTime
import java.awt.Color

@Command(name = "stats")
class CommandStats : BaseCommand(CommandCategory.ADMIN) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        var guilds = 0
        val users = mutableSetOf<String>()

        for (shard in Bot.shardManager.shards) {
            guilds += shard.guilds.size
            users.addAll(shard.users.map { it.id })
        }

        context.send().embed("Bot Statistics") {
            color = Color.BLUE
            fields {
                field {
                    title = "Servers"
                    inline = true
                    description = guilds.toString()
                }
                if (Bot.numShards > 1) {
                    field {
                        title = "Shard"
                        inline = true
                        description = "${context.shard.id} / ${Bot.numShards}"
                    }
                }
                field {
                    title = "Users"
                    inline = true
                    description = users.size.toString()
                }
                field {
                    title = "Uptime"
                    inline = true
                    description = localizeTime(
                            ((System.currentTimeMillis() - Bot.startTime) / 1000).toInt())
                }
                field {
                    title = "Version"
                    inline = !context.author.globalAdmin
                    description = buildString {
                        if (!context.author.globalAdmin) {
                            append(Bot.gitProperties.getProperty("git.build.version", "Unknown"))
                        } else {
                            appendln("__Branch__: ${Bot.gitProperties["git.branch"]}")
                            appendln("__Built At__: ${Bot.gitProperties["git.build.time"]}")
                            append("__Commit__: ${Bot.gitProperties["git.commit.id"]}")
                            if (Bot.gitProperties.getProperty("git.dirty", "false")!!.toBoolean()) {
                                append("*\n")
                            } else {
                                append("\n")
                            }
                            appendln("__Message__: `${Bot.gitProperties["git.commit.message.short"]}`")
                        }
                    }
                }
                field {
                    title = "URL"
                    inline = true
                    description = Bot.constants.getProperty("bot-base-url")
                }
            }
        }.rest().queue()
    }
}