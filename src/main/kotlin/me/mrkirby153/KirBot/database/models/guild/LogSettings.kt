package me.mrkirby153.KirBot.database.models.guild


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import java.sql.Timestamp

@Table("log_settings")
@Timestamps
class LogSettings : Model() {

    var id: String = ""

    @Column("server_id")
    var serverId: String = ""

    @Column("channel_id")
    var channelId: String = ""

    var included: Long = 0L

    var excluded: Long = 0L

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null
}