package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canInteractWith
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import java.util.concurrent.TimeUnit


class CommandBan {

    @Command(name = "ban", arguments = ["<user:snowflake>", "[reason:string...]"],
            clearance = CLEARANCE_MOD,
            permissions = [Permission.BAN_MEMBERS], category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Bans a user")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val member = context.guild.getMemberById(
                cmdContext.get<String>("user") ?: throw CommandException(
                        "Please specify a user"))

        var id: String? = member?.user?.id
        fun doBan() {
            val reason = cmdContext.get<String>("reason")

            if (member != null) {
                // Check perms if the user is a member of the server
                if (!context.guild.selfMember.canInteract(member))
                    throw CommandException("I cannot ban this user")
                if (!context.author.canInteractWith(context.guild, member.user))
                    throw CommandException("Missing permissions")
            }
            Infractions.ban(id!!, context.guild, context.author.id, reason,
                    0).handle { result, t ->
                if (t != null || !result.successful) {
                    val msg = if (t != null) t.message else result.errorMessage
                    val userMsg = member?.user?.logName ?: id
                    context.send().error("Could not ban $userMsg: `$msg`").queue()
                    return@handle
                }
                context.send().success(
                        "Banned ${member?.user?.logName ?: id}" + buildString {
                            if (reason != null)
                                append(" (`$reason`)")
                            when (result.dmResult) {
                                Infractions.DmResult.SENT ->
                                    append(" _Successfully messaged the user_")
                                Infractions.DmResult.SEND_ERROR ->
                                    append(" _Could not send DM to user._")
                                else -> {
                                }
                            }
                        }, true).queue()
            }
        }

        if (member == null) {
            context.channel.sendMessage(
                    ":warning: User is not a member of this server. Do you want to forceban them?").queue { m ->
                WaitUtils.confirmYesNo(m, context.author, {
                    id = cmdContext.getNotNull("user")
//                    m.delete().queue()
                    doBan()
                }, {
                    m.editMessage("Canceled!").queue {
                        it.deleteAfter(30, TimeUnit.SECONDS)
                    }
                })
            }
        } else {
            doBan()
        }


    }
}


class CommandUnban {
    @Command(name = "unban", arguments = ["<user:snowflake>", "[reason:string...]"],
            clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS],
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Unbans a user")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Please specify a user")
        val reason = cmdContext.get<String>("reason") ?: ""
        Infractions.unban(user, context.guild, context.author.id, reason).handle { result, t ->
            if (t != null || !result.successful) {
                context.send().error("An error occurred when unbanning the user: ${t
                        ?: result.errorMessage}").queue()
                return@handle
            }
            context.send().success("Unbanned `$user`", true).queue()
        }
    }
}

class CommandTempban {

    @Command(name = "tempban",
            arguments = ["<user:snowflake>", "<time:string>", "[reason:string...]"],
            clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS],
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Temporarily bans a user")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user")!!
        val reason = cmdContext.get<String>("reason")
        val timeRaw = cmdContext.get<String>("time") ?: "0"

        val time = Time.parse(timeRaw)
        if (time < 0)
            throw CommandException("Specify a duration greater than zero")
        val resolvedUser = context.guild.getMemberById(user)?.user
        if (resolvedUser != null) {
            if (!context.author.canInteractWith(context.guild, resolvedUser))
                throw CommandException("Missing permissions")
        }
        Infractions.tempban(user, context.guild, context.author.id, time, TimeUnit.MILLISECONDS,
                reason).handle { result, t ->
            if (t != null || !result.successful) {
                context.send().error(
                        "An error occurred when tempbanning ${resolvedUser?.logName ?: user}: ${t
                                ?: result.errorMessage}").queue()
                return@handle
            }
            context.send().success(buildString {
                append(resolvedUser?.logName ?: user)
                append(" has been temp-banned for ${Time.format(1, time)}: ")
                append("`$reason`")
                when (result.dmResult) {
                    Infractions.DmResult.SENT ->
                        append(" _Successfully messaged the user_")
                    Infractions.DmResult.SEND_ERROR ->
                        append(" _Could not send DM to user._")
                    else -> {
                    }
                }
            }, true).queue()
        }
    }

}

class CommandSoftban {

    @Command(name = "softban", arguments = ["<user:snowflake>", "[reason:string...]"],
            clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS],
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    @CommandDescription("Soft-bans (kicks and deletes the last 7 days) a user")
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user")!!
        val reason = cmdContext.get<String>("reason")
        val resolvedUser = context.guild.getMemberById(user)?.user
        if (resolvedUser != null) {
            if (!context.author.canInteractWith(context.guild, resolvedUser))
                throw CommandException("Missing permissions")
        }
        Infractions.softban(user, context.guild, context.author.id).handle { result, t ->
            if (t != null || !result.successful) {
                context.send().error(
                        "An error occurred when softbanning ${resolvedUser?.logName ?: user}: ${t
                                ?: result.errorMessage}").queue()
                return@handle
            }
            context.send().success("Soft-banned ${resolvedUser?.logName ?: user}" + buildString {
                if (reason != null)
                    append(": `$reason`")
                when (result.dmResult) {
                    Infractions.DmResult.SENT ->
                        append(" _Successfully messaged the user_")
                    Infractions.DmResult.SEND_ERROR ->
                        append(" _Could not send DM to user._")
                    else -> {
                    }
                }
            }, true).queue()
        }

    }

}