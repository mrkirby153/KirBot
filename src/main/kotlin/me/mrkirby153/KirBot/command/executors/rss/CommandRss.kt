package me.mrkirby153.KirBot.command.executors.rss

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.rss.FeedItem
import me.mrkirby153.KirBot.database.models.rss.RssFeed
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.Time
import java.net.URL
import java.util.concurrent.TimeUnit

@Command("rss")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandRss :
        BaseCommand(false, CommandCategory.MISCELLANEOUS, Arguments.string("action", false),
                Arguments.restAsString("parameters")) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val action = cmdContext.get<String>("action")?.toLowerCase()
        val parameters: List<String> = cmdContext.get<String>("parameters")?.split(
                " ")?.filter { it.isNotEmpty() } ?: emptyList()
        if (action == null || action == "list") {
            // Display feeds in this channel
            val feeds = Model.get(RssFeed::class.java,
                    Pair("server_id", context.guild.id))
            context.send().embed("RSS Feeds") {
                description {
                    +"The following RSS Feeds are registered on this channel: \n\n"
                    feeds.filter { it.channelId == context.channel.id }.forEach {
                        +"`${it.id}`"
                        +" - "
                        +it.url
                        if (!it.failed) {
                            +" (Last Update: "
                            if (it.lastCheck != null) {
                                +Time.format(1, System.currentTimeMillis() - it.lastCheck!!.time)
                                +" Ago"
                            } else
                                +"Never"
                            +")"
                        } else {
                            +" (Failed, must be refreshed manually)"
                        }
                        +"\n"
                    }
                    +"\n\n"
                    +"Use `${cmdPrefix}rss remove <id>` to delete the feed\n"
                    +"Use `${cmdPrefix}rss add <url>` to add a feed\n"
                    +"Use `${cmdPrefix}rss refresh <id>` to force a refresh"
                }
            }.rest().queue()
            return
        }

        when (action) {
            "add" -> {
                if (parameters.isEmpty())
                    throw CommandException("Please specify a URL")
                val rssFeed = parameters[0]
                // Make a new RSS feed
                val feed = RssFeed()
                feed.id = Model.randomId()
                feed.channelId = context.channel.id
                feed.serverId = context.guild.id
                feed.url = rssFeed
                feed.save()
                // Load new feeds async
                Bot.scheduler.schedule({
                    try {
                        val url = URL(feed.url)

                        val input = SyndFeedInput()
                        val f = input.build(XmlReader(url))
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
                context.send().success(
                        "Feed has been registered, new posts will be posted here").queue()
            }
            "remove" -> {
                if (parameters.isEmpty())
                    throw CommandException("Please specify an ID")
                // TODO 12/15/2017 Check if the RSS feed actually exists
                val feed = Model.first(RssFeed::class.java,
                        parameters[0]) ?: throw CommandException("That feed does not exist")
                feed.delete()
                context.send().success("Deleted RSS feed!").queue {
                    it.delete().queueAfter(10, TimeUnit.SECONDS)
                    context.message.delete().queueAfter(10, TimeUnit.SECONDS)
                }
            }
            "refresh" -> {
                if (parameters.isEmpty()) {
                    FeedTask.checkFeeds(context.guild)
                    context.send().success("Refreshed RSS feeds").queue {
                        it.delete().queueAfter(10, TimeUnit.SECONDS)
                        context.message.delete().queueAfter(10, TimeUnit.SECONDS)
                    }
                } else {
                    val feed = Model.first(RssFeed::class.java,
                            parameters[0]) ?: throw CommandException("That feed doesn't exist")
                    FeedTask.checkFeed(feed)
                    context.send().success("Refreshed RSS feed").queue {
                        it.delete().queueAfter(10, TimeUnit.SECONDS)
                        context.message.delete().queueAfter(10, TimeUnit.SECONDS)
                    }
                }
            }
        }
    }
}