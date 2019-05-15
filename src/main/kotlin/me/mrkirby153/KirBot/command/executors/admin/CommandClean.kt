package me.mrkirby153.KirBot.command.executors.admin

import com.mrkirby153.bfs.sql.QueryBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import me.mrkirby153.KirBot.utils.deleteAfter
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@Command(name = "clean", clearance = CLEARANCE_MOD)
@LogInModlogs
@CommandDescription("Cleans (deletes) messages")
class CommandClean :
        BaseCommand(false, CommandCategory.ADMIN) {

    private val confirmAmount = 100

    override fun execute(context: Context, cmdContext: CommandContext) {
        // Do nothing.
    }

    @Command(name = "all", arguments = ["[amount:int]"], clearance = CLEARANCE_MOD)
    @CommandDescription("Cleans messages from everyone in the current channel")
    fun allClean(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount") ?: 50
        if (amount > confirmAmount) {
            confirmClean(context, context.channel as TextChannel, amount)
        } else {
            doClean(context.channel as TextChannel, amount)
        }
    }

    @Command(name = "bots", arguments = ["[amount:int]"], clearance = CLEARANCE_MOD)
    @CommandDescription("Clean messages sent by bots in the current channel")
    fun botClean(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount") ?: 50
        if (amount > confirmAmount) {
            confirmClean(context, context.channel as TextChannel, amount, bots = true)
            return
        }
        doClean(context.channel as TextChannel, amount, bots = true)
    }

    @Command(name = "user", arguments = ["<user:snowflake>", "[amount:int]"],
            clearance = CLEARANCE_MOD)
    @CommandDescription("Clean messages sent by a specific user in the current channel")
    fun userClean(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user")!!
        val amount = cmdContext.get<Int>("amount") ?: 50
        if (amount > confirmAmount) {
            confirmClean(context, context.channel as TextChannel, amount, user, false)
            return
        }
        doClean(context.channel as TextChannel, amount, user, false)
    }

    private fun confirmClean(context: Context, channel: TextChannel, amount: Int,
                             user: String? = null,
                             bots: Boolean = false) {
        context.send().info(
                ":warning: Whoa, you're about to delete $amount messages. Are you sure you want to do this?").queue { msg ->
            msg.addReaction(GREEN_TICK.emote).queue()
            msg.addReaction(RED_TICK.emote).queue()
            WaitUtils.waitFor(GuildMessageReactionAddEvent::class.java) {
                if (it.user.id != context.author.id)
                    return@waitFor
                if (it.messageId != msg.id)
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
            }
        }
    }

    fun doClean(channel: TextChannel, amount: Int, user: String? = null,
                bots: Boolean = false) {
        if (amount <= 1)
            throw CommandException("Specify a number greater than 1")
        if (user != null) {
            Bot.LOG.debug("Performing user clean $user")
        } else if (bots) {
            Bot.LOG.debug("Performing bot clean")
        } else {
            Bot.LOG.debug("Performing all clean")
        }
        val now = Instant.now().minus(Duration.ofDays(14))

        val builder = QueryBuilder()
        builder.table("server_messages")
        builder.where("channel", channel.id)
        builder.where("deleted", false)
        builder.leftJoin("seen_users", "server_messages.author", "=", "seen_users.id")
        builder.select("server_messages.id")
        builder.orderBy("server_messages.id", "DESC")
        builder.limit(amount.toLong())

        if (bots)
            builder.where("bot", true)
        if (user != null)
            builder.where("author", user)

        val rows = builder.query()
        Bot.LOG.debug("Matched ${rows.size} messages")
        val ids = rows.mapNotNull { it.getString("id") }
        channel.purgeMessagesById(ids)
    }
}