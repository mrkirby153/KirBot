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
import net.dv8tion.jda.core.entities.User

@Command(name = "ban", arguments = ["<user:user>", "<reason:string,rest>"], clearance = Clearance.BOT_MANAGER)
@LogInModlogs
class CommandBan : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user")

        if(user.getMember(context.guild) == null)
            throw CommandException("That user could not be found")

        val reason = cmdContext.get<String>("reason") ?: throw CommandException(
                "Please specify a reason")

        if (!context.guild.selfMember.canInteract(user.getMember(context.guild)))
            throw CommandException("I cannot kick this user")
        if (user.getClearance(context.guild).value > context.author.getClearance(
                        context.guild).value)
            throw CommandException("You cannot kick this user")

        Infractions.ban(user.id, context.guild, context.author, reason, 7)
        context.send().success("Banned **${user.name}#${user.discriminator}** (`${user.id}`) (`$reason`)", true).queue()
    }
}

@Command(name = "forceban", arguments = ["<user:snowflake>", "<reason:string,rest>"], clearance = Clearance.BOT_MANAGER)
@LogInModlogs
class CommandForceBan : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Please specify a user")
        val reason = cmdContext.get<String>("reason") ?: throw CommandException(
                "Please specify a reason")

        Infractions.ban(user, context.guild, context.author, reason, 7)
        context.send().success("Banned `$user` (`$reason`)", true).queue()
    }
}

@Command(name = "unban", arguments = ["<user:snowflake>"], clearance = Clearance.BOT_MANAGER)
@LogInModlogs
class CommandUnban : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Please specify a user")

        Infractions.unban(user, context.guild, context.author)
        context.send().success("Unbanned `$user`", true).queue()
    }
}