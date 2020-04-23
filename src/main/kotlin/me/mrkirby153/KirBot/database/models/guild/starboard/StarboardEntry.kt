package me.mrkirby153.KirBot.database.models.guild.starboard


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.AutoIncrementing
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import java.sql.Timestamp

@Table("starboard")
@Timestamps
class StarboardEntry : Model() {

    var id = ""

    @Column("star_count")
    var count = 0L

    var hidden = false

    @Column("starboard_mid")
    var starboardMid: String? = null

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null

}