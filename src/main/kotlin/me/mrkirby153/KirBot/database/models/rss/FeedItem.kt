package me.mrkirby153.KirBot.database.models.rss

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("rss_feed_items")
class FeedItem : Model() {

    init {
        this.incrementing = false
    }

    @PrimaryKey
    var id = ""

    @Column("rss_feed_id")
    var feedId = ""

    var guid = ""
}