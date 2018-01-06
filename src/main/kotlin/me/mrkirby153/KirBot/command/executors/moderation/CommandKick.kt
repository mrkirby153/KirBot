package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.entities.User
import java.awt.Color

@Command("kick")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandKick : BaseCommand(false, CommandCategory.MODERATION, Arguments.user("user"),
        Arguments.string("reason", false)) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user")

        val reason = cmdContext.get<String>("reason") ?: "No reason specified"

        val member = user.getMember(context.guild)
        if (context.guild.selfMember.canInteract(member))
            context.guild.controller.kick(member,
                    "${context.author.name}#${context.author.discriminator} - $reason").queue {
                context.success()
                context.data.logManager.genericLog("User Kicked",
                        "${user.name}#${user.discriminator} was kicked by ${context.author.asMention}.\n\n**Reason:** $reason",
                        Color.RED, context.author)
            }
        else
            throw CommandException("I cannot kick this user")
    }
}