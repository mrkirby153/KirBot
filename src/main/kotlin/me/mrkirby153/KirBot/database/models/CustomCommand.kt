package me.mrkirby153.KirBot.database.models

@Table("custom_commands")
@AutoIncrementing(false)
class CustomCommand : Model() {

    @PrimaryKey
    var id = ""

    @Column("server")
    var serverId = ""

    var name = ""

    var data = ""

    @Column("clearance_level")
    var clearance = 0

    @Column("respect_whitelist")
    var respectWhitelist: Boolean = true

}