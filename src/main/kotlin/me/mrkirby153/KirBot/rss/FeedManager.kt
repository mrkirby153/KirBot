package me.mrkirby153.KirBot.rss

import com.mrkirby153.bfs.model.Model
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import me.mrkirby153.KirBot.database.models.rss.FeedItem
import me.mrkirby153.KirBot.database.models.rss.RssFeed
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import java.net.URL

object FeedManager {

    private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)

    fun registerFeed(url: String, channel: TextChannel) {
        val urlObj = URL(url)
        val feedBuilder = SyndFeedInput()
        val feed = feedBuilder.build(XmlReader(urlObj))

        val feedObject = RssFeed()
        feedObject.id = idGenerator.generate(10)
        feedObject.channelId = channel.id
        feedObject.serverId = channel.guild.id
        feedObject.url = url
        val existingEntries = mutableListOf<FeedItem>()
        // Register existing feed items
        feed.entries.forEach { entry ->
            val i = FeedItem()
            i.id = idGenerator.generate(10)
            i.feedId = feedObject.id
            i.guid = entry.uri
            existingEntries.add(i)
        }

        // Successfully processed the feed, commit everything to the DB
        feedObject.save()
        existingEntries.forEach { it.save() }
    }

    fun getFeeds(channel: TextChannel) = Model.where(RssFeed::class.java, "channel_id",
            channel.id).get()

    fun getFeeds(guild: Guild) = Model.where(RssFeed::class.java, "server_id", guild.id).get()

    fun getPostedFeedEntries(rssFeed: RssFeed) = Model.where(FeedItem::class.java, "rss_feed_id",
            rssFeed.id).get()

    fun getUnpostedFeedEntries(rssFeed: RssFeed): List<UnpostedFeedItem> {
        val f = SyndFeedInput().build(XmlReader(URL(rssFeed.url)))
        val posted = getPostedFeedEntries(rssFeed)
        return f.entries.filter { it.uri !in posted.map { it.guid } }.map {
            UnpostedFeedItem(rssFeed, it)
        }
    }

    class UnpostedFeedItem(val feed: RssFeed, val entry: SyndEntry) {

        fun markPosted() {
            val item = FeedItem()
            item.id = idGenerator.generate(10)
            item.feedId = feed.id
            item.guid = entry.uri
            item.save()
        }
    }
}