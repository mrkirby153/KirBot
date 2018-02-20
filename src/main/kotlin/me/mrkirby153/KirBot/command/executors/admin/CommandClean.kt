package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.deleteAfter
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

@Command("clean")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandClean :
        BaseCommand(false, CommandCategory.ADMIN, Arguments.number("messages", min = 2),
                Arguments.user("user", false)) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("messages")!!

        if (!context.channel.checkPermissions(Permission.MESSAGE_MANAGE)) {
            throw CommandException(
                    "I need the `Manager Messages` permission to use this command here")
        }

        if (context.channel is TextChannel) {
            val channel = context.channel as TextChannel
            var amountLeft = amount // The amount of messages we need to delete

            val waitMessage = channel.sendMessage(
                    ":arrows_counterclockwise: Please wait while I delete up to $amount messages" + buildString {
                        if (cmdContext.has("user")) {
                            val user = cmdContext.get<User>("user")
                            append(" from `" + user!!.name + "#" + user.discriminator + "`")
                        }
                    } + "...").complete()
            var deletedMessages = 0
            val history = channel.history
            while (amountLeft > 0) {
                val messages = history.retrievePast(100).complete().filter {
                    it.creationTime.isAfter(
                            OffsetDateTime.now().minusDays(
                                    14)) // We can only delete messages 14 days old
                }.filter {
                            it.id != waitMessage.id // We don't want to delete the wait message
                        }
                // Filter these messages for matching query
                val filtered = messages.filter {
                    if (cmdContext.has("user")) {
                        it.author == cmdContext.get("user")
                    } else {
                        true
                    }
                }
                if (filtered.isEmpty()) {
                    // We found no messages, we're done here
                    break
                }

                // Of the remaining, we need to retrieve max the amount to delete
                val toDelete = filtered.subList(0,
                        Math.min(Math.min(amountLeft, filtered.size), 99))

                amountLeft -= toDelete.size
                deletedMessages += toDelete.size

                if (toDelete.size == 1) {
                    // Just delete the first message cos we can't bulk delete it
                    toDelete[0].delete().queue()
                } else {
                    channel.deleteMessages(toDelete).queue()
                }
            }
            waitMessage.editMessage(":ok_hand: Deleted $deletedMessages messages").queue {
                it.deleteAfter(10, TimeUnit.SECONDS)
            }
        } else {
            throw CommandException("This should never happen - CommandClean")
        }
    }
}