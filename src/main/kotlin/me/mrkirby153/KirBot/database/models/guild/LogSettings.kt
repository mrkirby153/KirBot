package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("log_settings")
class LogSettings : Model() {

    var id: String = ""

    @Column("server_id")
    var serverId: String = ""

    @Column("channel_id")
    var channelId: String = ""

    var included: Long = 0L

    var excluded: Long = 0L
}