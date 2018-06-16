package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("role_permissions")
class RoleClearance : Model() {
    @Column("server_id")
    var serverId: String = ""

    @Column("role_id")
    var roleId: String = ""

    @Column("permission_level")
    var permission: Int = 0
}