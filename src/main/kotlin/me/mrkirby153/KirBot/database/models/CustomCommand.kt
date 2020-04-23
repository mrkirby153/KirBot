package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.PrimaryKey
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import java.sql.Timestamp

@Table("custom_commands")
@Timestamps
class CustomCommand : Model() {

    @PrimaryKey
    var id = ""

    @Column("server")
    var serverId = ""

    var name = ""

    var data = ""

    @Column("clearance_level")
    var clearance = 0

    var type: String = "TEXT"

    @Column("respect_whitelist")
    var respectWhitelist: Boolean = true

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null

}