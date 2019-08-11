package me.mrkirby153.KirBot.rss

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.rss.RssFeed
import me.mrkirby153.KirBot.utils.embed.b
import net.dv8tion.jda.api.entities.Guild
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
        fun checkFeeds(guild: Guild, ignoreFailed: Boolean = false) {
            val builder = Model.where(RssFeed::class.java, "server_id", guild.id)
            if (!ignoreFailed)
                builder.where("failed", false)
            builder.get().forEach { checkFeed(it) }
        }

        fun checkFeed(feed: RssFeed): Boolean {
            try {
                val unposted = FeedManager.getUnpostedFeedEntries(feed)
                Bot.LOG.debug("Feed ${feed.id} has ${unposted.size} entries that need to be posted")

                unposted.forEach {
                    feed.channel?.sendMessage(it.entry.link)?.queue()
                    it.markPosted()
                }

                feed.lastCheck = Timestamp(System.currentTimeMillis())
                feed.failed = false
                feed.save()
            } catch (e: Exception) {
                // An error occurred
                feed.channel?.sendMessage(buildString {
                    appendln(b("> FEED ERROR <"))
                    appendln(
                            "There was an error processing the feed `${feed.url}` (${feed.id}): `$e`")
                    appendln(
                            "It will not be checked automatically again and will need to be manually refreshed.")
                })?.queue()
                feed.failed = true
                feed.lastCheck = Timestamp(System.currentTimeMillis())
                feed.save()
                return false
            }
            return true
        }
    }
}