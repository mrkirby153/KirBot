package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canInteractWith

@Command(name = "warn", arguments = ["<user:snowflake>", "<reason:string...>"])
@CommandDescription("Register a warning infraction for the given user")
class CommandWarn : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<String>("user")!!
        val reason = cmdContext.get<String>("reason")!!
        if (reason.isEmpty())
            throw CommandException("Provide a reason!")

        val member = context.guild.getMemberById(userId)
        if (member != null) {
            if (!context.author.canInteractWith(context.guild, member.user))
                throw CommandException("Missing permissions")
        }
        Infractions.warn(userId, context.guild, context.author.id, reason)
        context.send().success(
                "Warned user ${Infractions.lookupUser(userId, true)} (`$reason`)", true).queue()
    }
}