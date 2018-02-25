package me.mrkirby153.KirBot.command.executors.rss

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.rss.FeedItem
import me.mrkirby153.KirBot.database.models.rss.RssFeed
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.kcutils.Time
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit

@Command(name = "rss", clearance = Clearance.BOT_MANAGER)
class CommandRss : BaseCommand(false, CommandCategory.MISCELLANEOUS) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        listFeeds(context, cmdContext)
    }

    @Command(name = "list", clearance = Clearance.BOT_MANAGER)
    fun listFeeds(context: Context, cmdContext: CommandContext) {
        val feeds = Model.get(RssFeed::class.java, Pair("server_id", context.guild.id))

        context.send().embed("RSS Feeds") {
            description {
                +"This channel is subscribed to the following feeds: \n\n"
                feeds.filter { it.channelId == context.channel.id }.forEach {
                    +"`${it.id}`"
                    +" - "
                    +it.url
                    if (!it.failed) {
                        +" (Last Update: "
                        if (it.lastCheck != null) {
                            +Time.format(1, System.currentTimeMillis() - it.lastCheck!!.time)
                            +" Ago"
                        } else {
                            +"Never"
                        }
                        +")"
                    } else {
                        +" (Failed. Must be refreshed manually)"
                    }
                    +"\n"
                }
                +"\n\n"
                +"Use `${cmdPrefix}rss remove <id>` to delete the feed\n"
                +"Use `${cmdPrefix}rss add <url>` to add a feed\n"
                +"Use `${cmdPrefix}rss refresh <id>` to refresh the feed now"
            }
        }.rest().queue()
    }

    @Command(name = "add", arguments = ["<url:string>"], clearance = Clearance.BOT_MANAGER)
    fun addFeed(context: Context, cmdContext: CommandContext) {
        val url = cmdContext.get<String>("url") ?: throw CommandException("Please provide a URL")

        // Check if the URL actually exists
        val req = Request.Builder().apply {
            url(url)
        }.build()

        val resp = HttpUtils.CLIENT.newCall(req).execute()

        if (resp.code() != 200) {
            throw CommandException(
                    "The URL you provided is invalid. (HTTP ${resp.code()} returned)")
        }

        // Create the feed
        val feed = RssFeed()
        feed.id = Model.randomId()
        feed.channelId = context.channel.id
        feed.serverId = context.guild.id
        feed.url = url
        feed.save()
        // Load all the posts so we don't spam with new posts
        Bot.scheduler.schedule({
            try {
                val feedUrl = URL(url)

                val input = SyndFeedInput()
                val f = input.build(XmlReader(feedUrl))
                f.entries.forEach {
                    val item = FeedItem()
                    item.id = Model.randomId()
                    item.feedId = feed.id
                    item.guid = it.uri
                    item.save()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }, 0, TimeUnit.MILLISECONDS)
        context.send().success("Feed has been registered, new posts will appear here").queue()
    }

    @Command(name = "remove", arguments = ["<id:string>"], clearance = Clearance.BOT_MANAGER)
    fun removeFeed(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<String>("id") ?: throw CommandException("Please provide a feed Id")

        val feed = Model.first(RssFeed::class.java, id)
        if (feed == null || feed.serverId != context.guild.id)
            throw CommandException("That feed does not exist")
        feed.delete()
        context.send().success("Deleted RSS Feed!").queue()
    }

    @Command(name = "refresh", arguments = ["[id:string]"], clearance = Clearance.BOT_MANAGER)
    fun refreshFeed(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("id")) {
            FeedTask.checkFeeds(context.guild)
            context.send().success("Refreshed RSS feeds").queue()
        } else {
            val id = cmdContext.get<String>("id")!!

            val feed = Model.first(RssFeed::class.java, id)

            if (feed == null || feed.serverId != context.guild.id)
                throw CommandException("That feed does not exist")

            FeedTask.checkFeed(feed)
            context.send().success("Refreshed RSS feed").queue()
        }
    }

}