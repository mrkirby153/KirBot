package com.mrkirby153.kirbot.command.mod

import com.mrkirby153.kirbot.services.InfractionService
import com.mrkirby153.kirbot.services.command.Command
import com.mrkirby153.kirbot.services.command.CommandException
import com.mrkirby153.kirbot.services.command.Commands
import com.mrkirby153.kirbot.services.command.context.CommandSender
import com.mrkirby153.kirbot.services.command.context.CurrentChannel
import com.mrkirby153.kirbot.services.command.context.CurrentGuild
import com.mrkirby153.kirbot.services.command.context.Optional
import com.mrkirby153.kirbot.services.command.context.Parameter
import com.mrkirby153.kirbot.services.command.context.Single
import com.mrkirby153.kirbot.utils.getMember
import com.mrkirby153.kirbot.utils.nameAndDiscrim
import com.mrkirby153.kirbot.utils.responseBuilder
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import java.util.concurrent.TimeUnit

@Commands
class ModerationCommands(val infractionService: InfractionService) {

    private fun getDmResultMessage(result: InfractionService.InfractionResult): String {
        return when (result.dmResult) {
            InfractionService.DmResult.SEND_ERROR -> "_Could not message the user_"
            InfractionService.DmResult.SENT -> "_The user has been successfully messaged_"
            else -> ""
        }
    }

    @Command(name = "kick", clearance = 50L, userPermissions = [Permission.KICK_MEMBERS],
            permissions = [Permission.KICK_MEMBERS], category = "moderation")
    fun kick(sender: CommandSender, guild: CurrentGuild, channel: CurrentChannel,
             @Parameter("user") member: Member,
             @Parameter("reason") @Optional reason: String?) {
        if (!guild.selfMember.canInteract(member)) {
            throw CommandException(
                    "I can't kick this user. Ensure that my highest role is above their highest role")
        }
        val sendMember = sender.getMember(guild) ?: throw CommandException(
                "You're not a member of this guild")
        if (!sendMember.canInteract(member)) {
            throw CommandException("You can't kick this user")
        }

        infractionService.kick(InfractionService.InfractionContext(member.user, guild, sender,
                reason)).handle { result, throwable ->
            if (throwable != null) {
                channel.responseBuilder.error(throwable.message ?: "An unknown error occurred")?.queue()
            }
            channel.responseBuilder.success(buildString {
                append("Kicked ${member.nameAndDiscrim} {{(`${member.id}`)}}")
                if (reason != null) {
                    append(": {{`$reason`}}")
                }
                if (result.infraction != null) {
                    append(" (#${result.infraction.id})")
                }
                append(" {{${getDmResultMessage(result)}}}")
            })?.queue()
        }
    }

    @Command(name = "ban", clearance = 50L, userPermissions = [Permission.BAN_MEMBERS],
            permissions = [Permission.BAN_MEMBERS], category = "moderation")
    fun ban(sender: CommandSender, guild: CurrentGuild, channel: CurrentChannel,
            @Parameter("user") member: Member, @Parameter("reason") @Optional reason: String?) {
        if (!guild.selfMember.canInteract(member))
            throw CommandException(
                    "I can't ban this user. Ensure my highest role is above their highest role")
        val sendMember = sender.getMember(guild) ?: throw CommandException(
                "You're not a member of this guild")
        if (!sendMember.canInteract(member)) {
            throw CommandException("You can't ban this user")
        }

        infractionService.ban(InfractionService.InfractionContext(member.user, guild, sender,
                reason)).handle { result, throwable ->
            if (throwable != null) {
                channel.responseBuilder.error(throwable.message ?: "An unknown error occurred")?.queue()
            }
            channel.responseBuilder.success(buildString {
                append("Banned ${member.nameAndDiscrim} {{(`${member.id}`)}}")
                if (reason != null) {
                    append(": {{`$reason`}}")
                }
                if (result.infraction != null) {
                    append(" (#${result.infraction.id})")
                }
                append(" {{${getDmResultMessage(result)}}}")
            })?.queue()
        }
    }

    @Command(name = "mute", clearance = 50L, userPermissions = [Permission.MANAGE_ROLES],
            permissions = [Permission.MANAGE_ROLES], category = "moderation")
    fun mute(sender: CommandSender, guild: CurrentGuild, channel: CurrentChannel,
             @Parameter("user") member: Member, @Parameter("reason") @Optional reason: String?) {
        if (!guild.selfMember.canInteract(member))
            throw CommandException(
                    "I can't mute this user. Ensure my highest role is above their highest role")
        val sendMember = sender.getMember(guild) ?: throw CommandException(
                "You're not a member of this guild")
        if (!sendMember.canInteract(member)) {
            throw CommandException("You can't mute this user")
        }

        infractionService.mute(InfractionService.InfractionContext(member.user, guild, sender,
                reason)).handle { result, throwable ->
            if (throwable != null) {
                channel.responseBuilder.error(throwable.message ?: "An unknown error occurred")?.queue()
            }
            channel.responseBuilder.success(buildString {
                append("Muted ${member.nameAndDiscrim} {{(`${member.id}`)}}")
                if (reason != null) {
                    append(": {{`$reason`}}")
                }
                if (result.infraction != null) {
                    append(" (#${result.infraction.id})")
                }
                append(" {{${getDmResultMessage(result)}}}")
            })?.queue()
        }
    }

    @Command(name = "warn", clearance = 50L, userPermissions = [Permission.KICK_MEMBERS],
            category = "moderation")
    fun warn(sender: CommandSender, guild: CurrentGuild, channel: CurrentChannel,
             @Parameter("user") member: Member, @Parameter("reason") @Optional reason: String?) {
        val sendMember = sender.getMember(guild) ?: throw CommandException(
                "You're not a member of this guild")
        if (!sendMember.canInteract(member)) {
            throw CommandException("You can't warn this user")
        }

        infractionService.warn(InfractionService.InfractionContext(member.user, guild, sender,
                reason)).handle { result, throwable ->
            if (throwable != null) {
                channel.responseBuilder.error(throwable.message ?: "An unknown error occurred")?.queue()
            }
            channel.responseBuilder.success(buildString {
                append("Warned ${member.nameAndDiscrim} {{(`${member.id}`)}}")
                if (reason != null) {
                    append(": {{`$reason`}}")
                }
                if (result.infraction != null) {
                    append(" (#${result.infraction.id})")
                }
                append(" {{${getDmResultMessage(result)}}}")
            })?.queue()
        }
    }

    @Command(name = "tempmute", clearance = 50L, userPermissions = [Permission.MANAGE_ROLES],
            permissions = [Permission.MANAGE_ROLES], category = "moderation")
    fun tempmute(sender: CommandSender, guild: CurrentGuild, channel: CurrentChannel,
                 @Parameter("user") member: Member, @Parameter("duration") @Single duration: String,
                 @Parameter("reason") @Optional reason: String?) {
        if (!guild.selfMember.canInteract(member))
            throw CommandException(
                    "I can't mute this user. Ensure my highest role is above their highest role")
        val sendMember = sender.getMember(guild) ?: throw CommandException(
                "You're not a member of this guild")
        if (!sendMember.canInteract(member)) {
            throw CommandException("You can't mute this user")
        }

        val durationMs = try {
            Time.parse(duration)
        } catch (e: IllegalArgumentException) {
            throw CommandException(e.message ?: "An unknown error occurred")
        }

        infractionService.tempMute(InfractionService.InfractionContext(member.user, guild.guild, sender,
                reason), durationMs, TimeUnit.MILLISECONDS).handle { result, throwable ->
            if (throwable != null) {
                throwable.printStackTrace()
                channel.responseBuilder.error(throwable.message ?: "An unknown error occurred")?.queue()
            }
            channel.responseBuilder.success(buildString {
                append("Temporarily muted ${member.nameAndDiscrim} {{(`${member.id}`)}} for ${Time.format(1, durationMs, smallest = Time.TimeUnit.SECONDS)}")
                if (reason != null) {
                    append(": {{`$reason`}}")
                }
                if (result.infraction != null) {
                    append(" (#${result.infraction.id})")
                }
                append(" {{${getDmResultMessage(result)}}}")
            })?.queue()
        }
    }

    @Command(name = "tempban", clearance = 50L, userPermissions = [Permission.BAN_MEMBERS],
            permissions = [Permission.BAN_MEMBERS], category = "moderation")
    fun tempban(sender: CommandSender, guild: CurrentGuild, channel: CurrentChannel,
                @Parameter("user") member: Member, @Parameter("duration") @Single duration: String,
                @Parameter("reason") @Optional reason: String?) {
        if (!guild.selfMember.canInteract(member))
            throw CommandException(
                    "I can't ban this user. Ensure my highest role is above their highest role")
        val sendMember = sender.getMember(guild) ?: throw CommandException(
                "You're not a member of this guild")
        if (!sendMember.canInteract(member)) {
            throw CommandException("You can't ban this user")
        }

        val durationMs = try {
            Time.parse(duration)
        } catch (e: IllegalArgumentException) {
            throw CommandException(e.message ?: "An unknown error occurred")
        }

        infractionService.tempBan(InfractionService.InfractionContext(member.user, guild, sender,
                reason), durationMs, TimeUnit.MILLISECONDS).handle { result, throwable ->
            if (throwable != null) {
                channel.responseBuilder.error(throwable.message ?: "An unknown error occurred")?.queue()
            }
            channel.responseBuilder.success(buildString {
                append("Temporarily banned ${member.nameAndDiscrim} {{(`${member.id}`)}} for ${Time.format(1, durationMs, smallest = Time.TimeUnit.SECONDS)}")
                if (reason != null) {
                    append(": {{`$reason`}}")
                }
                if (result.infraction != null) {
                    append(" (#${result.infraction.id})")
                }
                append(" {{${getDmResultMessage(result)}}}")
            })?.queue()
        }
    }
}