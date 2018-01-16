package me.mrkirby153.KirBot.database.models.rss

import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Table

@Table("rss_feed_items")
class FeedItem : Model() {

    var id = ""

    @Column("rss_feed_id")
    var feedId = ""

    var guid = ""
}