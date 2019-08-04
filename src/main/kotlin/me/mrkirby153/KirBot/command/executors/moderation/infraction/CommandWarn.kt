package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canInteractWith


class CommandWarn {

    @Command(name = "warn", arguments = ["<user:snowflake>", "<reason:string...>"],
            category = CommandCategory.MODERATION, clearance = CLEARANCE_MOD)
    @CommandDescription("Register a warning infraction for the given user")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<String>("user")!!
        val reason = cmdContext.get<String>("reason")!!
        if (reason.isEmpty())
            throw CommandException("Provide a reason!")

        val member = context.guild.getMemberById(userId)
        if (member != null) {
            if (!context.author.canInteractWith(context.guild, member.user))
                throw CommandException("Missing permissions")
        }
        val r = Infractions.warn(userId, context.guild, context.author.id, reason)
        context.send().success(buildString {
            append("Warned user ")
            append(Infractions.lookupUser(userId, true))
            append(" (`$reason`)")
            when (r.second) {
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