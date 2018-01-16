package me.mrkirby153.KirBot.database.models

@Table("quotes")
class Quote : Model() {

    var id = ""

    @Column("server_id")
    var serverId = ""

    @Column("message_id")
    var messageId = ""

    var user = ""

    var content = ""
}