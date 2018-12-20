package me.mrkirby153.KirBot.database.models.guild.starboard

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("starboard")
class StarboardEntry : Model() {

    var id = ""

    @Column("star_count")
    var count = 0L

    var hidden = false

    @Column("starboard_mid")
    var starboardMid: String? = null

    init {
        incrementing = false
    }
}