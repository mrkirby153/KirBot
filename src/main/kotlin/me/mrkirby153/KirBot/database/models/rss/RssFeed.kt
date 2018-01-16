package me.mrkirby153.KirBot.database.models.rss

import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Table
import java.sql.Timestamp

@Table("rss_feeds")
class RssFeed : Model() {

    var id = ""

    var channelId = ""

    var serverId = ""

    var url = ""

    var failed = false

    var lastCheck = Timestamp(0)
}