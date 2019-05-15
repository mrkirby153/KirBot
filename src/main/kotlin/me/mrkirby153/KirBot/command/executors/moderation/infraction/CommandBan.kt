package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canInteractWith
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit


class CommandBan {

    @Command(name = "ban", arguments = ["<user:user>", "[reason:string...]"],
            clearance = CLEARANCE_MOD,
            permissions = [Permission.BAN_MEMBERS], category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Bans a user")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user")

        if (user.getMember(context.guild) == null)
            throw CommandException("That user could not be found")

        val reason = cmdContext.get<String>("reason")

        if (!context.guild.selfMember.canInteract(user.getMember(context.guild)))
            throw CommandException("I cannot ban this user")
        if (!context.author.canInteractWith(context.guild, user))
            throw CommandException("Missing permissions")
        val r = Infractions.ban(user.id, context.guild, context.author.id, reason, 0)
        if (!r.first)
            throw CommandException("An error occurred when banning that user")

        context.send().success(
                "Banned **${user.name}#${user.discriminator}** (`${user.id}`)" + buildString {
                    if (reason != null)
                        append(" (`$reason`)")
                    when (r.second) {
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


class CommandForceBan {

    @Command(name = "forceban", arguments = ["<user:snowflake>", "[reason:string...]"],
            clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS],
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Force bans a user")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Please specify a user")
        val reason = cmdContext.get<String>("reason")

        // Check permissions if the user is in the guild
        val resolvedUser = context.guild.getMemberById(user)?.user
        if (resolvedUser != null) {
            if (!context.author.canInteractWith(context.guild, resolvedUser))
                throw CommandException("Missing permissions")
        }
        val r = Infractions.ban(user, context.guild, context.author.id, reason, 0)
        if (!r.first)
            throw CommandException("An error occurred when banning the user")
        context.send().success("Banned `$user` ${buildString {
            if (reason != null)
                append("(`$reason`)")
            when (r.second) {
                Infractions.DmResult.SENT ->
                    append(" _Successfully messaged the user_")
                Infractions.DmResult.SEND_ERROR ->
                    append(" _Could not send DM to user._")
                else -> {
                }
            }
        }}", true).queue()
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
        val r = Infractions.unban(user, context.guild, context.author.id, reason)
        if (!r)
            throw CommandException("An error occurred when unbanning the user")
        context.send().success("Unbanned `$user`", true).queue()
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
        val r = Infractions.tempban(user, context.guild, context.author.id, time,
                TimeUnit.MILLISECONDS,
                reason)
        if (!r.first)
            throw CommandException("An error occurred when tempbanning the user")
        context.send().success(buildString {
            append(resolvedUser?.logName ?: user)
            append(" has been temp-banned for ${Time.format(1, time)}: ")
            append("`$reason`")
            when (r.second) {
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
        val r = Infractions.softban(user, context.guild, context.author.id)
        if (!r.first)
            throw CommandException("An error occurred when softbanning")
        context.send().success("Soft-banned ${resolvedUser?.logName ?: user}" + buildString {
            if (reason != null)
                append(": `$reason`")
            when (r.second) {
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