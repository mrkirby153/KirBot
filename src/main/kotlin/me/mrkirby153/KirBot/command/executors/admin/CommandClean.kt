package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
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
                    ":arrows_counterclockwise: Please wait while I delete up to $amount messages...").complete()
            var deletedMessages = 0
            while (amountLeft > 0) {
                val amountToDelete = Math.min(amountLeft, 100) // Delete 100 max
                amountLeft -= amountToDelete
                Bot.LOG.debug("Attempting to delete $amountToDelete messages")
                channel.history.retrievePast(amountToDelete).queue { messages ->
                    val m = messages.filter {
                        // We can only delete messages in the last two weeks
                        it.creationTime.isAfter(OffsetDateTime.now().minusDays(14))
                    }.filter {
                                // Lets not delete the waiting message
                                it.id != waitMessage.id
                            }.filter {
                                // A user was passed in
                                if (cmdContext.has("user")) {
                                    it.author == cmdContext.get("user")
                                } else {
                                    true
                                }
                            }

                    // We didn't retrieve any more messages
                    if (m.isEmpty()) {
                        // We're done, there are no messages left to delete
                        waitMessage.editMessage(
                                ":ok_hand: Deleted $deletedMessages messages").queue {
                            it.deleteAfter(30, TimeUnit.SECONDS)
                        }
                        return@queue
                    }
                    // Increment the messages we've deleted
                    deletedMessages += m.size
                    if (m.size == 1) {
                        // We can't bulk delete two messages, so lets just delete the first one
                        m[0].delete().queue()
                        waitMessage.editMessage(
                                ":ok_hand: Deleted $deletedMessages messages").queue {
                            it.deleteAfter(30, TimeUnit.SECONDS)
                        }
                    }
                    // Delete all the messages
                    channel.deleteMessages(m).queue {
                        if (amountLeft <= 0) {
                            waitMessage.editMessage(
                                    ":ok_hand: Deleted $deletedMessages messages").queue {
                                it.deleteAfter(30, TimeUnit.SECONDS)
                            }
                        }
                    }
                }
            }
        } else {
            throw CommandException("This should never happen - CommandClean")
        }
    }
}