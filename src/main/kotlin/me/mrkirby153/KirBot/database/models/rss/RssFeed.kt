package me.mrkirby153.KirBot.database.models.rss

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.TextChannel
import java.sql.Timestamp

@Table("rss_feeds")
class RssFeed : Model() {

    init {
        this.incrementing = false
    }

    @PrimaryKey
    var id = ""

    @Column("channel_id")
    var channelId = ""

    @Column("server_id")
    var serverId = ""

    @Column("feed_url")
    var url = ""

    var failed = false

    var lastCheck : Timestamp? = null

    val channel: TextChannel?
        get() = Bot.shardManager.getGuild(serverId)?.getTextChannelById(this.channelId)
}