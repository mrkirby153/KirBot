package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.globalAdmin
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.entities.Guild
import java.awt.Color


class CommandStats {

    @Command(name = "stats", category = CommandCategory.UTILITY)
    @CommandDescription("Displays statistics about the bot")
    fun execute(context: Context, cmdContext: CommandContext) {
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
                field {
                    title = "Shard"
                    inline = true
                    description = context.jda.shardInfo.shardId.toString()
                }
                field {
                    title = "Users"
                    inline = true
                    description = users.size.toString()
                }
                field {
                    title = "Messages"
                    inline = true
                    description = "${messages.values.sum()} total. ${messages[context.guild.id]} in this server"
                }
                field {
                    title = "Uptime"
                    inline = true
                    description = Time.formatLong(System.currentTimeMillis() - Bot.startTime)
                }
                field {
                    title = "Version"
                    inline = !context.author.globalAdmin
                    description = buildString {
                        val gitProperties = Bot.gitProperties
                        if (gitProperties == null) {
                            append("_Build information currently unavailable_")
                        } else {
                            if (!context.author.globalAdmin) {
                                append(gitProperties.getProperty("git.build.version", "Unknown"))
                            } else {
                                appendln("__Branch__: ${gitProperties["git.branch"]}")
                                append("__Commit__: ${gitProperties["git.commit.id"]}")
                                if (gitProperties.getProperty("git.dirty", "false")!!.toBoolean()) {
                                    append("*\n")
                                } else {
                                    append("\n")
                                }
                                appendln(
                                        "__Message__: `${gitProperties["git.commit.message.short"]}`")
                            }
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

    companion object {
        var messages = mutableMapOf<String, Long>()

        fun incMessage(guild: Guild) {
            messages[guild.id] = (messages[guild.id] ?: 0) + 1
        }
    }
}