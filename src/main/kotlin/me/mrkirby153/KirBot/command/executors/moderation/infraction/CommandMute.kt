package me.mrkirby153.KirBot.command.executors.moderation.infraction

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
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit

@Command(name = "mute,shutup,quiet", arguments = ["<user:user>", "[reason:string...]"],
        clearance = CLEARANCE_MOD, permissions = [Permission.MANAGE_ROLES])
@LogInModlogs
class CommandMute : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
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
                "could not get the muted role")
        if (mutedRole.position > highest)
            throw CommandException("cannot assign the muted role")

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
                            TimeUnit.MILLISECONDS, r)
                    context.send().success("Muted **${user.nameAndDiscrim}** for ${Time.format(1,
                            time)}" + (if (r != null) " (`$r`)" else ""), true).queue()
                } catch (e: IllegalArgumentException) {
                    throw CommandException(e.message ?: "An unknown error occurred")
                }
                return
            }
        }
        Infractions.mute(user.id, context.guild, context.author.id, reason)
        context.send().success(
                "Muted **${user.name}#${user.discriminator}**" + (if (reason != null) "(`$reason`)" else ""),
                true).queue()
    }
}
