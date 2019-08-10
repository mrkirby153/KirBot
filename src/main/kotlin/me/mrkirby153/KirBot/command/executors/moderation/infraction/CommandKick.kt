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
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User


class CommandKick {

    @Command(name = "kick", arguments = ["<user:user>", "[reason:string...]"],
            clearance = CLEARANCE_MOD, permissions = [Permission.KICK_MEMBERS], category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    @CommandDescription("Kick a user")
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user")

        val reason = cmdContext.get<String>("reason")

        if (!context.guild.selfMember.canInteract(user.getMember(context.guild)))
            throw CommandException("I cannot kick this user")
        if (!context.author.canInteractWith(context.guild, user))
            throw CommandException("Missing permissions")
       Infractions.kick(user.id, context.guild, context.author.id, reason).handle { result, t ->
           if(t != null || !result.successful) {
               context.send().error("An error occurred when kicking ${user.logName}: ${t?: result.errorMessage}")
               return@handle
           }
           context.send().success("Kicked **${user.name}#${user.discriminator}** ${buildString {
               if (reason != null)
                   append("(`$reason`)")
               when(result.dmResult) {
                   Infractions.DmResult.SENT ->
                       append(" _Successfully messaged the user_")
                   Infractions.DmResult.SEND_ERROR ->
                       append(" _Could not send DM to user._")
                   else -> {}
               }
           }}", true).queue()
       }
    }
}