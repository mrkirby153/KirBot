package me.mrkirby153.KirBot.command.executors.rss

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import me.mrkirby153.KirBot.command.*
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.RssFeed
import me.mrkirby153.KirBot.rss.FeedTask
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import java.net.URL
import java.util.concurrent.TimeUnit

@Command("rss")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandRss : BaseCommand(false, CommandCategory.MISCELLANEOUS, Arguments.string("action", false), Arguments.restAsString("parameters")) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val action = cmdContext.get<String>("action")?.toLowerCase()
        val parameters: List<String> = cmdContext.get<String>("parameters")?.toLowerCase()?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()
        context.channel.sendTyping().queue()
        if (action == null || action == "list") {
            // Display feeds in this channel
            RssFeed.get(context.guild).queue { feeds ->
                context.send().embed("RSS Feeds") {
                    description {
                        +"The following RSS Feeds are registered on this channel: \n\n"
                        feeds.filter { it.channelId == context.channel.id }.forEach {
                            +"`${it.id}`"
                            +" - "
                            +it.url
                            if (!it.failed) {
                                +" (Last Update: "
                                if (it.lastCheck != null)
                                    +it.lastCheck.toString()
                                else
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
            }
            return
        }

        when (action) {
            "add" -> {
                if (parameters.isEmpty())
                    throw CommandException("Please specify a URL")
                val rssFeed = parameters[0]
                // Make a new RSS feed
                RssFeed.create(rssFeed, context.channel.id, context.guild).queue { feed ->
                    // Push old posts into the database
                    try {
                        val url = URL(feed.url)

                        val input = SyndFeedInput()
                        val f = input.build(XmlReader(url))
                        f.entries.filter { !feed.isPosted(it.uri) }.forEach {
                            feed.posted(it.uri)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    context.send().success("Feed has been registered, new posts will be posted here").queue()
                }
            }
            "remove" -> {
                if (parameters.isEmpty())
                    throw CommandException("Please specify an ID")
                // TODO 12/15/2017 Check if the RSS feed actually exists
                RssFeed.get(parameters[0]).queue {
                    it.delete().queue()
                }
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
                    RssFeed.get(parameters[0]).queue { feed ->
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
}