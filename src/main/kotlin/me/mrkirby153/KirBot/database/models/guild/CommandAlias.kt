package me.mrkirby153.KirBot.database.models.guild


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import java.sql.Timestamp

@Table("command_aliases")
@Timestamps
class CommandAlias : Model() {

    var id: String = ""

    @Column("server_id")
    var serverId: String = ""

    var command: String = ""
        get() = field.toLowerCase()

    var alias: String? = ""

    var clearance: Int = 0

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null
}