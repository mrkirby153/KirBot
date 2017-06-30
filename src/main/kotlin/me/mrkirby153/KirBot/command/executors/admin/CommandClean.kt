package me.mrkirby153.KirBot.command.executors.admin


import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

@Command(name = "clean", clearance = Clearance.BOT_MANAGER, description = "Delete the last messages in the context",
        requiredPermissions = arrayOf(Permission.MESSAGE_MANAGE, Permission.MESSAGE_WRITE), category = "Moderation")
class CommandClean : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.number("amount")!!.toInt()

        if(context.channel is TextChannel){
            val channel = context.channel
            channel.history.retrievePast(amount).queue { m ->
                val m1 = m.filter { it.creationTime.isAfter(OffsetDateTime.now().minusDays(14)) }
                channel.deleteMessages(m1).queue{
                    context.send().success("Deleted $amount messages!").queue {
                        it.delete().queueAfter(10, TimeUnit.SECONDS)
                    }
                }
            }
        } else {
            // Should never happen
            System.err.println("Attempting to clean a channel which is not a TextChannel. Is ${context.channel.javaClass.canonicalName}")
            throw CommandException("This should never happen!")
        }
    }
}