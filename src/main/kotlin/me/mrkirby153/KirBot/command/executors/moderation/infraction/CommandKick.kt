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
import net.dv8tion.jda.core.entities.User

@Command(name = "kick", arguments = ["<user:user>", "[reason:string...]"],
        clearance = CLEARANCE_MOD)
@LogInModlogs
class CommandKick : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user")

        val reason = cmdContext.get<String>("reason")

        if (!context.guild.selfMember.canInteract(user.getMember(context.guild)))
            throw CommandException("I cannot kick this user")
        if (!context.author.canInteractWith(context.guild, user))
            throw CommandException("Missing permissions")
        if(!context.guild.selfMember.hasPermission(Permission.KICK_MEMBERS))
            throw CommandException("cannot kick members on this guild")
        Infractions.kick(user.id, context.guild, context.author.id, reason)
        context.send().success("Kicked **${user.name}#${user.discriminator}** ${buildString {
            if (reason != null)
                append("(`$reason`)")
        }}",
                true).queue()
    }
}