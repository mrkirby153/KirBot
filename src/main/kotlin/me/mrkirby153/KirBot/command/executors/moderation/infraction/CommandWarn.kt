package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canInteractWith
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import javax.inject.Inject


class CommandWarn @Inject constructor(private val infractions: Infractions) {

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
        infractions.warn(userId, context.guild, context.author.id, reason).handle { result, t ->
            if (t != null || !result.successful) {
                context.send().error("An error occurred when warning ${member?.user?.nameAndDiscrim
                        ?: userId}: ${t ?: result.errorMessage}").queue()
                return@handle
            }
            context.send().success(buildString {
                append("Warned user ")
                append(infractions.lookupUser(userId, true))
                append(" (`$reason`)")
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