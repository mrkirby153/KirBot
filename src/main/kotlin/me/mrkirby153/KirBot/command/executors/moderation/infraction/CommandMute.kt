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
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "mute,shutup,quiet", arguments = ["<user:user>", "[reason:string...]"],
        clearance = CLEARANCE_MOD)
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

        if (!context.guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
            throw CommandException("cannot assign the muted role on this guild")

        val highest = context.guild.selfMember.roles.map { it.position }.max() ?: 0
        val mutedRole = Infractions.getMutedRole(context.guild) ?: throw CommandException(
                "could not get the muted role")
        if (mutedRole.position > highest)
            throw CommandException("cannot assign the muted role")

        Infractions.mute(user.id, context.guild, context.author.id, reason)
        context.send().success(
                "Muted **${user.name}#${user.discriminator}**" + (if (reason != null) "(`$reason`)" else ""),
                true).queue()
    }
}
