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
import com.mrkirby153.kirbot.utils.getMember
import com.mrkirby153.kirbot.utils.nameAndDiscrim
import com.mrkirby153.kirbot.utils.responseBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

@Commands
class ModerationCommands(val infractionService: InfractionService) {

    private fun getDmResultMessage(result: InfractionService.InfractionResult): String {
        return when (result.dmResult) {
            InfractionService.DmResult.SEND_ERROR -> "_Could not message the user_"
            InfractionService.DmResult.SENT -> "_The user has been successfully messaged_"
            else -> ""
        }
    }

    @Command(name = "kick", clearance = 100L, userPermissions = [Permission.KICK_MEMBERS],
            permissions = [Permission.KICK_MEMBERS], category = "moderation")
    fun kick(sender: CommandSender, guild: CurrentGuild, channel: CurrentChannel,
             @Parameter("user") user: Member,
             @Parameter("reason") @Optional reason: String?) {
        if (!guild.selfMember.canInteract(user)) {
            throw CommandException(
                    "I can't kick this user. Ensure that my highest role is above theirs")
        }
        val sendMember = sender.getMember(guild) ?: throw CommandException(
                "You're not a member of this guild")
        if (!sendMember.canInteract(user)) {
            throw CommandException("You can't kick this user")
        }

        infractionService.kick(InfractionService.InfractionContext(user.user, guild, sender,
                reason)).handle { result, throwable ->
            if (throwable != null) {
                throw CommandException(throwable.message ?: "An unknown error occurred")
            }
            channel.responseBuilder.success(buildString {
                append("Kicked ${user.nameAndDiscrim} {{(`${user.id}`)}}")
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