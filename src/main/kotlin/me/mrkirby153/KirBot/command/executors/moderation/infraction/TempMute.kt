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
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit


class TempMute {

    @Command(name = "tempmute", arguments = ["<user:user>", "<time:string>", "[reason:string...]"],
            permissions = [Permission.MANAGE_ROLES], clearance = CLEARANCE_MOD,
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Temporarily mute the given user")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user")!!
        val member = user.getMember(context.guild) ?: throw CommandException(
                "This user isn't a part of the guild!")

        val time = cmdContext.get<String>("time")!!

        val reason = cmdContext.get<String>("reason")

        val timeParsed = Time.parse(time)

        if (timeParsed <= 0) {
            throw CommandException("Please provide a time greater than 0")
        }

        if (context.channel !is TextChannel)
            throw CommandException("This command won't work in PMs")

        if (!context.author.canInteractWith(context.guild, user))
            throw CommandException("Missing permissions")

        val highest = context.guild.selfMember.roles.map { it.position }.max() ?: 0
        val mutedRole = Infractions.getMutedRole(context.guild) ?: throw CommandException(
                "could not get the muted role")
        if (mutedRole.position > highest)
            throw CommandException("cannot assign the muted role")

        Infractions.tempMute(user.id, context.guild, context.author.id, timeParsed,
                TimeUnit.MILLISECONDS, reason).handle { result, t ->
            if (t != null || !result.successful) {
                context.send().error("Could not temp-mute ${user.nameAndDiscrim}: ${t
                        ?: result.errorMessage}").queue()
                return@handle
            }
            context.send().success(
                    "Muted ${user.nameAndDiscrim} for ${Time.format(1, timeParsed)}" + buildString {
                        if (reason != null) {
                            append(" (`$reason`)")
                        }
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