package me.mrkirby153.KirBot.database.models

@Table("custom_commands")
class CustomCommand : Model() {

    @PrimaryKey
    var id = ""

    @Column("server")
    var serverId = ""

    var name = ""

    var data = ""

    @Column("clearance")
    private var clearanceRaw = ""

    @Column("respect_whitelist")
    var respectWhitelist: Boolean = true
}