package me.mrkirby153.KirBot.command.executors.moderation.infraction

import com.sun.org.glassfish.gmbal.Description
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
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

@Command(name = "ban", arguments = ["<user:user>", "[reason:string...]"], clearance = CLEARANCE_MOD,
        permissions = [Permission.BAN_MEMBERS])
@LogInModlogs
@CommandDescription("Bans a user")
class CommandBan : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user")

        if (user.getMember(context.guild) == null)
            throw CommandException("That user could not be found")

        val reason = cmdContext.get<String>("reason")

        if (!context.guild.selfMember.canInteract(user.getMember(context.guild)))
            throw CommandException("I cannot ban this user")
        if (!context.author.canInteractWith(context.guild, user))
            throw CommandException("Missing permissions")
        Infractions.ban(user.id, context.guild, context.author.id, reason, 0)
        context.send().success(
                "Banned **${user.name}#${user.discriminator}** (`${user.id}`)" + buildString {
                    if (reason != null)
                        append(" (`$reason`)")
                }, true).queue()
    }
}

@Command(name = "forceban", arguments = ["<user:snowflake>", "[reason:string...]"],
        clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS])
@LogInModlogs
@CommandDescription("Force bans a user")
class CommandForceBan : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Please specify a user")
        val reason = cmdContext.get<String>("reason")

        // Check permissions if the user is in the guild
        val resolvedUser = context.guild.getMemberById(user)?.user
        if (resolvedUser != null) {
            if (!context.author.canInteractWith(context.guild, resolvedUser))
                throw CommandException("Missing permissions")
        }
        Infractions.ban(user, context.guild, context.author.id, reason, 0)
        context.send().success("Banned `$user` ${buildString {
            if (reason != null)
                append("(`$reason`)")
        }}", true).queue()
    }
}

@Command(name = "unban", arguments = ["<user:snowflake>", "[reason:string...]"],
        clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS])
@LogInModlogs
@CommandDescription("Unbans a user")
class CommandUnban : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Please specify a user")
        val reason = cmdContext.get<String>("reason") ?: ""
        Infractions.unban(user, context.guild, context.author.id, reason)
        context.send().success("Unbanned `$user`", true).queue()
    }
}

@Command(name = "tempban", arguments = ["<user:snowflake>", "<time:string>", "[reason:string...]"],
        clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS])
@LogInModlogs
@CommandDescription("Temporarily bans a user")
class CommandTempban : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
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
                reason)
        context.send().success(
                "${resolvedUser?.logName ?: user} has been temp-banned for ${Time.format(1, time)}", true).queue()
    }

}

@Command(name = "softban", arguments = ["<user:snowflake>", "[reason:string...]"], clearance = CLEARANCE_MOD, permissions = [Permission.BAN_MEMBERS])
@LogInModlogs
@CommandDescription("Soft-bans (kicks and deletes the last 7 days) a user")
class CommandSoftban: BaseCommand(false, CommandCategory.MODERATION){
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user")!!
        val reason = cmdContext.get<String>("reason")
        val resolvedUser = context.guild.getMemberById(user)?.user
        if (resolvedUser != null) {
            if (!context.author.canInteractWith(context.guild, resolvedUser))
                throw CommandException("Missing permissions")
        }
        Infractions.softban(user, context.guild, context.author.id)
        context.send().success("Soft-banned ${resolvedUser?.logName ?: user}" + buildString {
            if(reason != null)
                append(": `$reason`")
        }, true).queue()
    }

}