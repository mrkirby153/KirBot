package me.mrkirby153.KirBot.command.executors.admin

import com.mrkirby153.bfs.sql.QueryBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class CommandClean @Inject constructor(private val shardManager: ShardManager) {

    private val confirmAmount = 100

    @Command(name = "all", arguments = ["[amount:int]"], parent = "clean",
            clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION, permissions = [Permission.MESSAGE_MANAGE])
    @CommandDescription("Cleans messages from everyone in the current channel")
    @LogInModlogs
    @IgnoreWhitelist
    fun allClean(context: Context, cmdContext: CommandContext) {
        val builder = cmdContext.getBuilder()
        builder.where("channel", context.channel.id)
        purgeMessages(context, builder.queryIds())
    }

    @Command(name = "user", arguments = ["<user:snowflake>", "[amount:int]"], parent = "clean",
            clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION, permissions = [Permission.MESSAGE_MANAGE])
    @CommandDescription("Cleans messages from a specific user in the current channel")
    @LogInModlogs
    @IgnoreWhitelist
    fun userClean(context: Context, cmdContext: CommandContext) {
        val builder = cmdContext.getBuilder()
        builder.where("channel", context.channel.id)
        builder.where("author", cmdContext.get<String>("user"))
        purgeMessages(context, builder.queryIds())
    }

    @Command(name = "bots", arguments = ["[amount:int]"], parent = "clean",
            clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION, permissions = [Permission.MESSAGE_MANAGE])
    @CommandDescription("Cleans messages sent by bots in the current channel")
    @LogInModlogs
    @IgnoreWhitelist
    fun botClean(context: Context, cmdContext: CommandContext) {
        val builder = cmdContext.getBuilder()
        builder.where("channel", context.channel.id)
        builder.leftJoin("seen_users", "server_messages.author", "=", "seen_users.id")
        builder.where("bot", true)
        purgeMessages(context, builder.queryIds())
    }

    @Command(name = "everywhere", arguments = ["<user:snowflake>", "[amount:int]"],
            parent = "clean", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Cleans messages sent by the given user across all channels")
    @IgnoreWhitelist
    fun cleanEverywhere(context: Context, cmdContext: CommandContext) {
        val builder = cmdContext.getBuilder()
        builder.where("server_id", context.guild.id)
        builder.where("author", cmdContext.getNotNull("user"))
        purgeMessages(context, builder.queryIds())
    }

    @Command(name = "between", arguments = ["<first:snowflake>", "<last:snowflake>"],
            parent = "clean", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Cleans messages sent by all users between the given snowflakes")
    @IgnoreWhitelist
    fun cleanBetween(context: Context, cmdContext: CommandContext) {
        val builder = getBuilder()
        builder.where("id", ">=", cmdContext.getNotNull("first"))
        builder.where("id", "<=", cmdContext.getNotNull("last"))
        builder.where("channel", context.channel.id)
        purgeMessages(context, builder.queryIds())
    }

    private fun purgeMessages(context: Context, messages: List<String>) {
        fun doClean() {
            val m = context.channel.sendMessage(":repeat: Processing...").complete()
            val start = System.currentTimeMillis()
            val buckets = mutableMapOf<String, MutableList<String>>()
            val builder = QueryBuilder()
            builder.table("server_messages")
            builder.whereIn("id", messages.toTypedArray())
            builder.select("id", "channel")
            builder.where("deleted", false)
            builder.query().forEach {
                buckets.getOrPut(it.getString("channel"), { mutableListOf() }).add(
                        it.getString("id"))
            }

            Bot.LOG.debug("Purging ${messages.size} messages across ${buckets.size} channels")
            val cf = mutableListOf<CompletableFuture<*>>()
            var failedChannels = 0
            buckets.forEach { (channelId, messages) ->
                val channel = shardManager.getTextChannelById(channelId)
                if (channel == null || !channel.checkPermissions(Permission.MESSAGE_MANAGE)) {
                    failedChannels++
                    return@forEach
                }
                cf.addAll(channel.purgeMessagesById(messages).toTypedArray())
            }
            CompletableFuture.allOf(*cf.toTypedArray()).handle { _, _ ->
                val msg = buildString {
                    append("Finished in `")
                    append(Time.format(1, System.currentTimeMillis() - start))
                    append("`. Deleted ")
                    append(messages.size)
                    append(" messages")
                    if (failedChannels > 0) {
                        append(". Could not delete messages in ")
                        append(failedChannels)
                        append(" channels")
                    }
                }
                m.editMessage(msg).queue()
            }
        }
        if (messages.size >= confirmAmount) {
            val msg = context.channel.sendMessage(
                    ":warning: You're about to delete ${messages.size} messages. Are you sure you want to do this?").complete()
            WaitUtils.confirmYesNo(msg, context.author, {
                msg.delete().queue()
                doClean()
            })
        } else {
            doClean()
        }
    }

    private fun getBuilder(amount: Long? = 50): QueryBuilder {
        return QueryBuilder().table("server_messages").where("deleted", false).select(
                "server_messages.id")
                .orderBy("server_messages.id", "DESC").apply {
                    if (amount != null)
                        limit(amount)
                }
    }

    private fun QueryBuilder.queryIds(): List<String> {
        return this.query().map { it.getString("id") }
    }

    private fun CommandContext.getBuilder(): QueryBuilder {
        return getBuilder(this.get<Int>("amount")?.toLong() ?: 50L)
    }
}