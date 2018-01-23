package me.mrkirby153.KirBot.database.models.rss

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.AutoIncrementing
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import net.dv8tion.jda.core.entities.TextChannel
import java.sql.Timestamp

@Table("rss_feeds")
@AutoIncrementing(false)
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

    val channel: TextChannel?
        get() = Bot.shardManager.getGuild(serverId)?.getTextChannelById(this.channelId)
}