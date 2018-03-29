package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import me.mrkirby153.KirBot.utils.deleteAfter
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

@Command(name = "clean", arguments = ["<amount:int,2,x>"], clearance = CLEARANCE_MOD)
@LogInModlogs
class CommandClean :
        BaseCommand(false, CommandCategory.ADMIN) {

    val confirmAmount = 100

    override fun execute(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount")!!
        if (amount > confirmAmount) {
            confirmClean(context, context.channel as TextChannel, amount)
        } else {
            doClean(context.channel as TextChannel, amount)
        }
    }

    @Command(name = "bots", arguments = ["<amount:int,2,x>"])
    fun botClean(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount")!!
        if (amount > confirmAmount) {
            confirmClean(context, context.channel as TextChannel, amount, bots = true)
            return
        }
        doClean(context.channel as TextChannel, amount, bots = true)
    }

    @Command(name = "user", arguments = ["<user:snowflake>", "<amount:int,2,x>"])
    fun userClean(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user")!!
        val amount = cmdContext.get<Int>("amount")!!
        if (amount > confirmAmount) {
            confirmClean(context, context.channel as TextChannel, amount, user, false)
            return
        }
        doClean(context.channel as TextChannel, amount, user, false)
    }

    private fun confirmClean(context: Context, channel: TextChannel, amount: Int, user: String? = null,
                             bots: Boolean = false) {
        context.send().info(
                ":warning: Whoa, you're about to delete $amount messages. Are you sure you want to do this?").queue { msg ->
            msg.addReaction(GREEN_TICK.emote).queue()
            msg.addReaction(RED_TICK.emote).queue()
            WaitUtils.waitFor(GuildMessageReactionAddEvent::class.java, {
                if (it.user.id != context.author.id)
                    return@waitFor
                if(it.messageId != msg.id)
                    return@waitFor
                if (it.reactionEmote.isEmote) {
                    when (it.reactionEmote.id) {
                        GREEN_TICK.id -> {
                            doClean(channel, amount, user, bots)
                            msg.delete().queue()
                        }
                        RED_TICK.id -> {
                            msg.editMessage("Canceled!").queue {
                                it.deleteAfter(10, TimeUnit.SECONDS)
                            }
                        }
                    }
                }
                cancel()
            })
        }
    }

    fun doClean(channel: TextChannel, amount: Int, user: String? = null,
                bots: Boolean = false) {
        var amountLeft = amount // The amount of messages we need to delete
        if (user != null) {
            Bot.LOG.debug("Performing user clean $user")
        } else if (bots) {
            Bot.LOG.debug("Performing bot clean")
        } else {
            Bot.LOG.debug("Performing all clean")
        }
        val waitMessage = channel.sendMessage(
                ":arrows_counterclockwise: Please wait while I delete up to $amount messages...").complete()
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
                if (user != null) {
                    it.author.id == user
                } else if (bots) {
                    it.author.isBot
                } else
                    true
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
            Bot.LOG.debug("Performing delete on ${toDelete.map { it.id }}")
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
    }
}