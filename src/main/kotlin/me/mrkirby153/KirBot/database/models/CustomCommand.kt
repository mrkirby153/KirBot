package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("custom_commands")
class CustomCommand : Model() {

    init {
        this.incrementing = false
    }

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