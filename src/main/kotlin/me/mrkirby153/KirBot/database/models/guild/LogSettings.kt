package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Table

@Table("log_settings")
class LogSettings : Model(){

    var id: String = ""

    @Column("server_id")
    var serverId: String = ""

    @Column("channel_id")
    var channelId: String = ""

    var included: Long = 0L

    var excluded: Long = 0L
}