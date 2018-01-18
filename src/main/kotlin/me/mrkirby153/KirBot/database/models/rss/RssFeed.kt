package me.mrkirby153.KirBot.database.models.rss

import me.mrkirby153.KirBot.database.models.AutoIncrementing
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import java.sql.Timestamp

@Table("rss_feeds")
@AutoIncrementing(false)
class RssFeed : Model() {

    @PrimaryKey
    var id = ""

    var channelId = ""

    var serverId = ""

    var url = ""

    var failed = false

    var lastCheck = Timestamp(0)
}