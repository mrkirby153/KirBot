package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.STATUS_AWAY
import me.mrkirby153.KirBot.utils.STATUS_DND
import me.mrkirby153.KirBot.utils.STATUS_OFFLINE
import me.mrkirby153.KirBot.utils.STATUS_ONLINE
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.getPrimaryColor
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Member
import java.text.SimpleDateFormat


class CommandServer {
    @Command(name = "server", arguments = ["[server:snowflake]"], clearance = 50,
            category = CommandCategory.FUN)
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val serverId = cmdContext.get<String>("server") ?: context.guild.id
        val server = Bot.shardManager.getGuildById(serverId) ?: throw CommandException(
                "Server not found")

        context.channel.sendMessage(embed {
            color = getPrimaryColor(server.iconUrl)
            thumbnail = server.iconUrl
            title {
                append(server.name)
            }
            description {
                appendln("Created: ${Time.format(1,
                        System.currentTimeMillis() - (server.creationTime.toEpochSecond() * 1000))} ago (${SimpleDateFormat(
                        Time.DATE_FORMAT_NOW).format(server.creationTime.toEpochSecond() * 1000)})")
                appendln("Members: ${server.members.size}")
                if (server.features.isNotEmpty())
                    appendln("Features: ${server.features.joinToString(", ")}")
                else
                    appendln("Features: none")
                appendln()
                appendln("Roles: ${server.roles.size}")
                appendln("Categories: ${server.categories.size}")
                appendln("Text Channels: ${server.textChannels.size}")
                appendln("Voice Channels: ${server.voiceChannels.size}")

                val memberFilter = mutableMapOf<OnlineStatus, MutableList<Member>>()
                server.members.forEach { member ->
                    memberFilter.getOrPut(member.onlineStatus) { mutableListOf() }.add(member)
                }
                appendln()
                appendln("$STATUS_ONLINE ${memberFilter[OnlineStatus.ONLINE]?.size ?: "0"}")
                appendln("$STATUS_AWAY ${memberFilter[OnlineStatus.IDLE]?.size ?: "0"}")
                appendln("$STATUS_DND ${memberFilter[OnlineStatus.DO_NOT_DISTURB]?.size ?: "0"}")
                appendln("$STATUS_OFFLINE ${memberFilter[OnlineStatus.OFFLINE]?.size ?: "0"}")
            }
        }.build()).queue()
    }
}