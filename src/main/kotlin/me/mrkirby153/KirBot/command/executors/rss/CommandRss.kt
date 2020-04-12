package me.mrkirby153.KirBot.command.executors.rss

import com.mrkirby153.bfs.model.Model
import com.rometools.rome.io.FeedException
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.rss.RssFeed
import me.mrkirby153.KirBot.rss.FeedManager
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.RED_TICK
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import okhttp3.Request


class CommandRss {

    @Command(name = "rss", clearance = CLEARANCE_MOD, category = CommandCategory.UTILITY, permissions = [Permission.MESSAGE_EMBED_LINKS])
    @CommandDescription("Shows a list of RSS feeds currently being monitored")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        listFeeds(context, cmdContext)
    }

    @Command(name = "list", clearance = CLEARANCE_MOD,
            permissions = [Permission.MESSAGE_EMBED_LINKS], parent = "rss", category = CommandCategory.UTILITY)
    @CommandDescription("Show a list of RSS feeds being monitored")
    @IgnoreWhitelist
    fun listFeeds(context: Context, cmdContext: CommandContext) {
        val cmdPrefix = GuildSettings.commandPrefix.get(context.guild)
        val feeds = Model.where(RssFeed::class.java, "server_id", context.guild.id).get()

        context.channel.sendMessage(buildString {
            appendln("<#${context.channel.id}> is subscribed to the following feeds:\n")
            if (feeds.isEmpty()) {
                appendln("_No feeds_")
            }
            feeds.filter { it.channelId == context.channel.id }.forEach {
                append("`${it.id}` - ${it.url} (Last Update: ")
                if (it.failed) {
                    append("Failed ${Time.format(1,
                            System.currentTimeMillis() - it.lastCheck!!.time)} ago, must be refreshed manually)")
                } else {
                    if (it.lastCheck != null) {
                        append(Time.format(1, System.currentTimeMillis() - it.lastCheck!!.time))
                        append(" ago)")
                    } else {
                        append("Never)")
                    }
                }
                appendln()
            }
            appendln()
            appendln("Use `${cmdPrefix}rss remove <id>` to delete the feed")
            appendln("Use `${cmdPrefix}rss add <url>` to add a feed")
            append("Use `${cmdPrefix}rss refresh <id>` to refresh the feed now")
        }).queue()
    }

    @Command(name = "add", arguments = ["<url:string>"], clearance = CLEARANCE_MOD, parent = "rss", category = CommandCategory.UTILITY)
    @CommandDescription("Adds a feed to be watched")
    @IgnoreWhitelist
    fun addFeed(context: Context, cmdContext: CommandContext) {
        val url = cmdContext.get<String>("url") ?: throw CommandException("Please provide a URL")

        // Sanity check to make sure they're not registering the same feed twice
        val existing = Model.query(RssFeed::class.java).where("server_id", context.id).where(
                "feed_url", url).where("channel_id", context.channel.id).first()
        if (existing != null)
            throw CommandException("A feed with that URL is already registered")

        // Check if the URL actually exists
        val req = Request.Builder().apply {
            url(url)
        }.build()

        val resp = HttpUtils.CLIENT.newCall(req).execute()

        if (resp.code() != 200) {
            throw CommandException(
                    "The URL you provided is invalid. (HTTP ${resp.code()} returned)")
        }

        context.kirbotGuild.runAsyncTask {
            val m = context.channel.sendMessage("Registering feed `$url`").complete()
            try {
                FeedManager.registerFeed(url, context.channel as TextChannel)
                m.editMessage(
                        "$GREEN_TICK Feed has been registered to <#${context.channel.id}>").queue()
            } catch (e: FeedException) {
                e.printStackTrace()
                m.editMessage("$RED_TICK An error occurred when parsing the feed").queue()
            } catch (e: IllegalArgumentException) {
                m.editMessage("$RED_TICK An invalid URL was provided").queue()
            } catch (e: Exception) {
                m.editMessage("$RED_TICK An unknown error occurred").queue()
            }
        }
    }

    @Command(name = "remove", arguments = ["<id:string>"], clearance = CLEARANCE_MOD, parent = "rss", category = CommandCategory.UTILITY)
    @CommandDescription("Removes a feed from the watch list")
    @IgnoreWhitelist
    fun removeFeed(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<String>("id") ?: throw CommandException("Please provide a feed Id")

        val feed = Model.where(RssFeed::class.java, "id", id).first()
        if (feed == null || feed.serverId != context.guild.id)
            throw CommandException("That feed does not exist")
        feed.delete()
        context.send().success("Deleted RSS Feed!").queue()
    }

    @Command(name = "refresh", arguments = ["[id:string]"], clearance = CLEARANCE_MOD, parent = "rss", category = CommandCategory.UTILITY)
    @CommandDescription("Refresh a feed")
    @IgnoreWhitelist
    fun refreshFeed(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("id")) {
            FeedTask.checkFeeds(context.guild)
            context.send().success("Refreshed RSS feeds").queue()
        } else {
            val id = cmdContext.get<String>("id")!!

            val feed = Model.where(RssFeed::class.java, "id", id).first()

            if (feed == null || feed.serverId != context.guild.id)
                throw CommandException("That feed does not exist")

            if (FeedTask.checkFeed(feed))
                context.send().success("Refreshed RSS feed").queue()
            else
                context.send().error("An error occurred when refreshing the RSS feed").queue()
        }
    }

}