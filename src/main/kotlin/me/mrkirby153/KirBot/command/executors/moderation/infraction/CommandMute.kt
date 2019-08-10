package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canInteractWith
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit


class CommandMute {
    @Command(name = "mute", arguments = ["<user:user>", "[reason:string...]"],
            clearance = CLEARANCE_MOD, permissions = [Permission.MANAGE_ROLES],
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Mute a user (Assign the set muted role)")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException(
                "Please specify a user to mute")
        val reason = cmdContext.get<String>("reason")
        val member = user.getMember(context.guild) ?: throw CommandException(
                "This user isn't a part of the guild!")
        if (context.channel !is TextChannel)
            throw CommandException("This command won't work in PMs")

        if (!context.author.canInteractWith(context.guild, user))
            throw CommandException("Missing permissions")

        val highest = context.guild.selfMember.roles.map { it.position }.max() ?: 0
        val mutedRole = Infractions.getMutedRole(context.guild) ?: throw CommandException(
                "The muted role is not configured on this server!")
        if (mutedRole.position > highest)
            throw CommandException("Cannot assign the muted role")

        if (reason != null) {
            val split = reason.split(" ")
            if (split[0].matches(Regex("((\\d+\\s?)(\\D+))+"))) {
                // This is a tempmute
                try {
                    val timeRaw = split[0]
                    val time = Time.parse(timeRaw)
                    if (time < 1)
                        throw CommandException("Please specify a duration greater than zero")
                    val r = if (split.size > 1) split.drop(1).joinToString(" ") else null

                    Infractions.tempMute(user.id, context.guild, context.author.id, time,
                            TimeUnit.MILLISECONDS, r).handle { result, t ->
                        if (t != null || !result.successful) {
                            context.send().error("Could not temp-mute ${user.nameAndDiscrim}: ${t
                                    ?: result.errorMessage}").queue()
                            return@handle
                        }
                        context.send().success(buildString {
                            append("Muted **")
                            append(user.logName)
                            append("** for ")
                            append(Time.format(1, time))
                            if (r != null)
                                append(" (`$r`)")
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
                } catch (e: IllegalArgumentException) {
                    throw CommandException(e.message ?: "An unknown error occurred")
                }
                return
            }
        }
        Infractions.mute(user.id, context.guild, context.author.id, reason).handle { result, t ->
            if (t != null || !result.successful) {
                context.send().error("An error occurred when muting ${user.nameAndDiscrim}: ${t
                        ?: result.errorMessage}").queue()
                return@handle
            }
            context.send().success(buildString {
                append("Muted ${user.logName}")
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
