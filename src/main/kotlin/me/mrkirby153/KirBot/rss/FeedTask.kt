package me.mrkirby153.KirBot.rss

import com.mrkirby153.bfs.model.Model
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.rss.FeedItem
import me.mrkirby153.KirBot.database.models.rss.RssFeed
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.inlineCode
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.entities.Guild
import java.awt.Color
import java.net.URL
import java.sql.Timestamp

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

        private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)

        fun checkFeeds(guild: Guild, ignoreFailed: Boolean = false) {
            val feeds = Model.where(RssFeed::class.java, "server_id", guild.id).get()
            feeds.filter { ignoreFailed || !it.failed }.forEach {
                checkFeed(it)
            }
        }

        fun checkFeed(feed: RssFeed) {
            try {
                val url = URL(feed.url)

                val input = SyndFeedInput()
                val f = input.build(XmlReader(url))

                val posted = Model.where(FeedItem::class.java, "rss_feed_id", feed.id).get().map { it.guid }
                f.entries.filter { it.uri !in posted }.forEach {
                    feed.channel?.sendMessage(it.link)?.queue()
                    val item = FeedItem()
                    item.id = this.idGenerator.generate(10)
                    item.feedId = feed.id
                    item.guid = it.uri
                    item.save()
                }
                feed.failed = false
                feed.lastCheck = Timestamp(System.currentTimeMillis())
                feed.save()
            } catch (e: Exception) {
                e.printStackTrace()
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
                feed.failed = true
                feed.lastCheck = Timestamp(System.currentTimeMillis())
                feed.save()
            }
        }
    }
}