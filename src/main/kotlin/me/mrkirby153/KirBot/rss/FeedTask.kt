package me.mrkirby153.KirBot.rss

import com.overzealous.remark.Options
import com.overzealous.remark.Remark
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.RssFeed
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.inlineCode
import me.mrkirby153.KirBot.utils.embed.link
import net.dv8tion.jda.core.entities.Guild
import java.awt.Color
import java.net.URL

class FeedTask : Runnable {

    override fun run() {
        Bot.shards.forEach { shard ->
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

                val remarkOpts = Options.markdown()
                remarkOpts.inlineLinks = true
                val remark = Remark(remarkOpts)
                f.entries.filter { !feed.isPosted(it.uri) }.forEach {
                    // Should be unposted feeds
                    val toConvert = if(it.description != null) it.description.value else "No description provided"
                    val msg = remark.convertFragment(toConvert)
                            .replace(Regex("\\s?#{1,4}\\s?"), "**").replace(Regex("!\\["), "[")

                    val embed = embed(it.title) {
                        color = Color.BLUE
                        description {
                            if (msg.length > 2000) {
                                +msg.substring(0, 2000)
                                +"..."
                            } else {
                                +msg
                            }
                            +"\n\n"
                            +("Link" link it.link)
                        }
                        footer {
                            text {
                                if (it.updatedDate != null)
                                    +it.updatedDate.toString()
                            }
                        }
                    }
                    feed.channel?.sendMessage(embed.build())?.queue()
                    feed.posted(it.uri)
                }
                feed.update(true).queue()
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
                feed.update(false).queue()
            }
        }
    }
}