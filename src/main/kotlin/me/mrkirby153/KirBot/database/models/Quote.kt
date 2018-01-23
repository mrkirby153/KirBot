package me.mrkirby153.KirBot.database.models

@Table("quotes")
class Quote : Model() {

    var id: Long? = null

    @Column("server_id")
    var serverId = ""

    @Column("message_id")
    var messageId = ""

    var user = ""

    var content = ""

    override fun toString(): String {
        return "Quote(id=$id, serverId='$serverId', messageId='$messageId', user='$user', content='$content')"
    }


}