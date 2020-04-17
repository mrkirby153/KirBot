package me.mrkirby153.KirBot.command.executors.`fun`

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.ReactionRole
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.ReactionRoles
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.FuzzyMatchException
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.inlineCode
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.escapeMarkdown
import me.mrkirby153.KirBot.utils.findMessage
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.sanitize
import net.dv8tion.jda.api.Permission
import java.awt.Color
import javax.inject.Inject

class CommandReactionRole @Inject constructor(private val reactionRoles: ReactionRoles) {

    val emojiRegex = Regex("<a?:.*:([0-9]*)>")


    @Command(name = "list", clearance = CLEARANCE_MOD, parent = "reaction-role",
            permissions = [Permission.MESSAGE_EMBED_LINKS])
    @CommandDescription("Lists the reaction roles configured on the server")
    fun listRoles(context: Context, cmdContext: CommandContext) {
        val roles = Model.where(ReactionRole::class.java, "guild_id", context.guild.id).get()

        if (roles.isEmpty()) {
            context.channel.sendMessage("No reaction roles are configured!").queue()
            return
        }

        var msg = ""
        roles.forEach { role ->
            val roleStr = buildString {
                append(b("Emote: "))
                append("${role.displayString} ")
                appendln(inlineCode { role.id })
                appendln("${b("Role: ")} ${role.role?.name?.escapeMarkdown() ?: "???"}")
                appendln("${b("Channel: ")} ${role.channel?.asMention ?: "???"}")
                appendln("${b("Message ID: ")} ${role.messageId}")
                appendln(
                        "Jump to Message".link(
                                "https://discordapp.com/channels/${role.guildId}/${role.channelId}/${role.messageId}"))
                appendln()
            }
            if (msg.length + roleStr.length >= 2040) {
                context.channel.sendMessage(embed {
                    description {
                        append(msg)
                    }
                    color = Color.BLUE
                }.build()).queue()
            } else {
                msg += roleStr
            }
        }
        context.channel.sendMessage(embed {
            description {
                append(msg)
            }
            color = Color.BLUE
        }.build()).queue()
    }

    @Command(name = "add", clearance = CLEARANCE_MOD, parent = "reaction-role",
            arguments = ["<mid:string>", "<emoji:string>", "<role:string...>"])
    @CommandDescription("Adds a reaction role")
    fun addRole(context: Context, cmdContext: CommandContext) {
        val messageRaw = cmdContext.getNotNull<String>("mid")
        val emojiRaw = cmdContext.getNotNull<String>("emoji")
        val roleRaw = cmdContext.getNotNull<String>("role")

        val role = try {
            context.guild.kirbotGuild.matchRole(roleRaw) ?: throw CommandException(
                    "No roles were found")
        } catch (e: FuzzyMatchException) {
            when (e) {
                is FuzzyMatchException.TooManyMatchesException -> throw CommandException(
                        "Too many roles for that search query. Try a more specific query")
                is FuzzyMatchException.NoMatchesException -> throw CommandException(
                        "No roles found for the given query")
                else -> throw CommandException(e.localizedMessage)
            }
        }

        // Check if we can use the custom emoji
        var custom = false
        var effectiveEmote = emojiRaw
        if (emojiRegex.matches(emojiRaw)) {
            custom = true
            val id = emojiRegex.find(emojiRaw)?.groups?.get(1)?.value ?: throw CommandException(
                    "Regex match failed. This shouldn't happen")
            var found = false
            Bot.shardManager.guilds.forEach { guild ->
                if (id in guild.emotes.map { it.id })
                    found = true
            }
            effectiveEmote = id
            if (!found) {
                throw CommandException("Cannot use that emote for reaction roles")
            }
        }

        context.channel.sendMessage("Looking up message...").queue { msg ->
            findMessage(messageRaw).handle { message, throwable ->
                if (throwable != null) {
                    if (throwable is NoSuchElementException) {
                        msg.editMessage("$RED_TICK The given message was not found!").queue()
                    } else {
                        msg.editMessage("$RED_TICK ${throwable.localizedMessage}").queue()
                    }
                    return@handle
                } else {
                    if(!message.channel.checkPermissions(Permission.MESSAGE_ADD_REACTION)) {
                        context.send().error("I cannot add reactions to the given message. You will have to add it manually").queue()
                    }
                    reactionRoles.addReactionRole(message, role,
                            effectiveEmote, custom)
                    msg.editMessage("$GREEN_TICK Added $emojiRaw as a reaction role for ${b(
                            role.name.sanitize())}").queue()
                }
            }
        }
    }

    @Command(name = "remove", clearance = CLEARANCE_MOD, parent = "reaction-role",
            arguments = ["<id:string>"])
    @CommandDescription("Removes a reaction role from the server")
    fun removeRole(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.getNotNull<String>("id")
        try {
            reactionRoles.removeReactionRole(id, context.guild)
            context.send().success("Reaction role has been removed").queue()
        } catch (e: IllegalArgumentException) {
            throw CommandException(e.localizedMessage)
        }
    }
}