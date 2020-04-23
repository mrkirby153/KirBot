package me.mrkirby153.KirBot.database.models


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import java.sql.Timestamp

@Table("role_permissions")
@Timestamps
class RoleClearance : Model() {

    var id: String = ""

    @Column("server_id")
    var serverId: String = ""

    @Column("role_id")
    var roleId: String = ""

    @Column("permission_level")
    var permission: Int = 0

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null
}