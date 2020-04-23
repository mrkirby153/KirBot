package me.mrkirby153.KirBot.database.models.rss

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.PrimaryKey
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.sharding.ShardManager
import java.sql.Timestamp

@Table("rss_feeds")
@Timestamps
class RssFeed : Model() {


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

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null

    val channel: TextChannel?
        get() = Bot.applicationContext.get(ShardManager::class.java).getGuildById(serverId)?.getTextChannelById(this.channelId)
}