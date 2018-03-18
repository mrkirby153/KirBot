package me.mrkirby153.KirBot.database.models

@Table("role_permissions")
class RoleClearance : Model() {
    @Column("server_id")
    var serverId: String = ""

    @Column("role_id")
    var roleId: String = ""

    @Column("permission_level")
    var permission: Int = 0
}