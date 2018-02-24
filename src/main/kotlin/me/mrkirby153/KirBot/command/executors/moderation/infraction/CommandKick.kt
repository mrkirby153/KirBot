package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.User

@Command(name = "kick", arguments = ["<user:user>", "<reason:string,rest>"], clearance = Clearance.BOT_MANAGER)
@LogInModlogs
class CommandKick : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user")

        val reason = cmdContext.get<String>("reason") ?: throw CommandException(
                "Please specify a reason")

        if (!context.guild.selfMember.canInteract(user.getMember(context.guild)))
            throw CommandException("I cannot kick this user")
        if (user.getClearance(context.guild).value > context.author.getClearance(
                        context.guild).value)
            throw CommandException("You cannot kick this user")
        Infractions.kick(user, context.guild, context.author, reason)
        context.kirbotGuild.logManager.genericLog(":boot:",
                "${user.name}#${user.discriminator} (`${user.id}`) was kicked by ${context.author.nameAndDiscrim} (`$reason`)")
        context.send().success("Kicked **${user.name}#${user.discriminator}** (`$reason`)",
                true).queue()
    }
}