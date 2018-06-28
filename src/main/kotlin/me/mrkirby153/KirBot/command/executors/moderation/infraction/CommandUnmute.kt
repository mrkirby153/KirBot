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

@Command(name = "unmute,unquiet", arguments = ["<user:user>", "[reason:string...]"],
        clearance = CLEARANCE_MOD, permissions = [Permission.MANAGE_ROLES])
@LogInModlogs
class CommandUnmute : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException(
                "Please specify a user to unmute!")
        val member = user.getMember(context.guild) ?: throw CommandException(
                "The user is not a member of this guild!")

        if (context.channel !is TextChannel) {
            throw CommandException("This command doesn't work in PMs")
        }
        if (!context.author.canInteractWith(context.guild, user))
            throw CommandException("Missing permissions")

        val highest = context.guild.selfMember.roles.map { it.position }.max() ?: 0
        val mutedRole = Infractions.getMutedRole(context.guild) ?: throw CommandException(
                "could not get the muted role")
        if (mutedRole.position > highest)
            throw CommandException("cannot un-assign the muted role")

        Infractions.unmute(user.id, context.guild, context.author.id, cmdContext.get("reason"))
        context.send().success("Unmuted **${user.name}#${user.discriminator}**", true).queue()
    }
}