package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.STATUS_AWAY
import me.mrkirby153.KirBot.utils.STATUS_DND
import me.mrkirby153.KirBot.utils.STATUS_OFFLINE
import me.mrkirby153.KirBot.utils.STATUS_ONLINE
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.getPrimaryColor
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import java.awt.Color
import java.text.SimpleDateFormat


class CommandServer {
    @Command(name = "server", arguments = ["[server:snowflake]"], clearance = CLEARANCE_MOD,
            category = CommandCategory.FUN, permissions = [Permission.MESSAGE_ATTACH_FILES])
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val serverId = cmdContext.get<String>("server") ?: context.guild.id
        val server = Bot.shardManager.getGuildById(serverId) ?: throw CommandException(
                "Server not found")

        context.channel.sendMessage(embed {
            color = if (server.iconUrl != null) getPrimaryColor(server.iconUrl!!) else Color(114,
                    137, 218)
            thumbnail = server.iconUrl
            title {
                append(server.name)
            }
            fields {
                field {
                    title = "ID"
                    description = context.guild.id
                    inline = true
                }
                field {
                    title = "Created"
                    description = " ${Time.format(1,
                            System.currentTimeMillis() - (server.timeCreated.toEpochSecond() * 1000))} ago (${SimpleDateFormat(
                            "MM-dd-yy HH:mm:ss").format(
                            server.timeCreated.toEpochSecond() * 1000)})"
                    inline = true
                }
                field {
                    title = "Members"
                    description = server.members.size.toString()
                    inline = true
                }
                field {
                    title = "Features"
                    description = if (server.features.isNotEmpty()) {
                        server.features.joinToString(", ")
                    } else {
                        "None"
                    }
                    inline = true
                }
                if(serverId == context.guild.id)
                    // Only show the roles on the current server
                    field {
                        title = "Roles"
                        description = server.roles.filter { !it.isPublicRole }.joinToString(" ") { it.asMention }
                    }
                field {
                    title = "Categories"
                    description = server.categories.size.toString()
                    inline = true
                }
                field {
                    title = "Text Channels"
                    description = server.textChannels.size.toString()
                    inline = true
                }
                field {
                    title = "Voice Channels"
                    description = server.voiceChannels.size.toString()
                    inline = true
                }
                field {
                    title = "Members"
                    val memberFilter = mutableMapOf<OnlineStatus, MutableList<Member>>()
                    server.members.forEach { member ->
                        memberFilter.getOrPut(member.onlineStatus) { mutableListOf() }.add(member)
                    }
                    description {
                        appendln("$STATUS_ONLINE Online: ${memberFilter[OnlineStatus.ONLINE]?.size ?: "0"}")
                        appendln("$STATUS_AWAY Idle: ${memberFilter[OnlineStatus.IDLE]?.size ?: "0"}")
                        appendln("$STATUS_DND Do not Disturb: ${memberFilter[OnlineStatus.DO_NOT_DISTURB]?.size ?: "0"}")
                        appendln("$STATUS_OFFLINE Offline: ${memberFilter[OnlineStatus.OFFLINE]?.size ?: "0"}")
                    }
                }
            }
        }.build()).queue()
    }
}