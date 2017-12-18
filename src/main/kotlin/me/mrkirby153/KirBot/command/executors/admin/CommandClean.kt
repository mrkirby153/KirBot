package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.*
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.deleteAfter
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

@Command("clean")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandClean : BaseCommand(false, CommandCategory.ADMIN, Arguments.number("messages", min = 2, max = 100), Arguments.user("user", false)) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("messages")!!
        if (!context.channel.checkPermissions(Permission.MESSAGE_MANAGE)) {
            context.send().error("I need the `Manage Messages` permission to use this command here!").queue {
                it.deleteAfter(10, TimeUnit.SECONDS)
                context.deleteAfter(10, TimeUnit.SECONDS)
            }
            return
        }
        if (context.channel is TextChannel) {
            val channel = context.channel as TextChannel
            channel.history.retrievePast(amount).queue { m ->
                val messages = m.filter { it.creationTime.isAfter(OffsetDateTime.now().minusDays(14)) }.filter {
                    if(cmdContext.has("user")){
                        it.author == cmdContext.get("user")
                    } else {
                        true
                    }
                }
                channel.deleteMessages(messages).queue {
                    context.send().success("Deleted ${messages.size} messages!").queue {
                        it.deleteAfter(10, TimeUnit.SECONDS)
                    }
                }
            }
        } else {
            throw CommandException("This should never happen!")
        }
    }
}