package me.mrkirby153.KirBot.rss

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.RssFeed
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.inlineCode
import net.dv8tion.jda.core.entities.Guild
import java.awt.Color
import java.net.URL

class FeedTask : Runnable {

    override fun run() {
        Bot.shardManager.shards.forEach { shard ->
            shard.guilds.forEach { guild ->
                // Get the feeds for the guild
                Bot.LOG.debug("Checking feeds for ${guild.id}")
                checkFeeds(guild)
            }
        }
    }

    companion object {
        fun checkFeeds(guild: Guild, ignoreFailed: Boolean = false) {
            RssFeed.get(guild).queue { feeds ->
                feeds.filter { ignoreFailed || !it.failed }.forEach { feed ->
                    // Retrieve the feed
                    checkFeed(feed)
                }
            }
        }

        fun checkFeed(feed: RssFeed) {
            try {
                val url = URL(feed.url)

                val input = SyndFeedInput()
                val f = input.build(XmlReader(url))

                f.entries.filter { !feed.isPosted(it.uri) }.forEach {
                    feed.channel?.sendMessage(it.link)?.queue()
                    feed.posted(it.uri)
                }
                feed.update(true).queue()
            } catch (e: Exception) {
                feed.channel?.sendMessage(embed("Feed Error") {
                    color = Color.RED
                    description {
                        +"There was an error processing the feed "
                        +inlineCode { feed.url }
                        +". It will not be checked automatically again and will need to be manually refreshed"
                        +"\n\n"
                        +"```$e```"
                    }
                }.build())?.queue()
                feed.update(false).queue()
            }
        }
    }
}